# Översikt
Detta projekt är en Java Spring Boot-applikation som importerar CSV-filer till AWS. Applikationen hanterar filuppladdning, databasskrivning och notifiering, körs helt i molnet
via Elastic Beanstalk(EC2).

### Flöde i korthet:
1. Källkod pushas till GitHub.
2. CodePipeline bygger via CodeBuild och deployar till Elastic Beanstalk (EC2).
3. Applikationen laddar upp CSV till S3 via presigned URL.
4. CSV-data läses och sparas till DynamoDB.
5. En notifikation skickas via SNS (Simple Notification Service).
6. AWS X-Ray spårar flödet och prestandan.
7. IAM-roller hanterar rätt åtkomst.
8. Step Functions används för att visa hur processen kan bearbetas i flera steg.

---
# Arkitektur
![Diagram skiss.png](image/Diagram%20skiss.png)

---

# AWS-tjänster i projektet
### S3 - Filuppladdning
* Tar emot CSV-fil via presigned URL.
* Bucket: csv-importer-bucket-tony.
![Imports.png](image/Imports.png)

### DynamoDB - Databas
* Tabell: DataItems.
* Varje Csv-rad sparas som id, name, age.
* Applikationen räknar antal importerade och misslyckade poster.
![i DataItems så finns alla 3 personer får csv filen.png](image/i%20DataItems%20s%C3%A5%20finns%20alla%203%20personer%20f%C3%A5r%20csv%20filen.png)
* ImportJobs i DynamoDB ⬇️ 
![ImportJobs table dynamodb finns min imp fil.png](image/ImportJobs%20table%20dynamodb%20finns%20min%20imp%20fil.png)

### SNS - Notifiering
* Topic csv-importer-events.
* Skickar en summering när importen är färdig (processed, failed, status).
* Kan kopplas till e-mail eller andra system (Jag körde e-mail).
![SNS overview.png](image/SNS%20overview.png)
![IMPORT STARTED i mailen.png](image/IMPORT%20STARTED%20i%20mailen.png)
![IMPORT FINISHED i mailen.png](image/IMPORT%20FINISHED%20i%20mailen.png)

### Step Functions
* Visar hur ett enkelt arbestflöde (ValidateInput -> PublishToSNS).
![step fucntions OK.png](image/step%20fucntions%20OK.png)

### Elastic Beanstalk (EC2)
* Kör applikationen automatiskt med lastbalansering och autocaling.
* All konfiguration för miljön hanteras genom EB -> Enviroment -> Software.
![Elastic beanstalk OK.png](image/Elastic%20beanstalk%20OK.png)
![EC2 instans körs OK.png](image/EC2%20instans%20k%C3%B6rs%20OK.png)
* Valde t3.micro i början vilket va lite för lite så den larmade hela tiden om för högt CPU bruk
![en liten varning för högt cpu användning men det är för att jag valde t3.micro.png](image/en%20liten%20varning%20f%C3%B6r%20h%C3%B6gt%20cpu%20anv%C3%A4ndning%20men%20det%20%C3%A4r%20f%C3%B6r%20att%20jag%20valde%20t3.micro.png)

### CodePipeline & CodeBuild
* Full CI/CD flöde:
  * Källa: GitHub.
  * Build: CodeBuild.
  * Deploy Elastic Beanstalk.
* Bygglogg och status syns direkt i AWS - Konsolen.
![uppdaterad pipeline med SNS.png](image/uppdaterad%20pipeline%20med%20SNS.png)

### X-Ray
* Spårar API anrop, S3 läsningar, DynamoDB skrivningar.
* Ger översikt över alla beroenden.
![trace map2.png](image/trace%20map2.png)
* I X-Ray:s trace map ser man hur applikationen kommunicerar mellan S3 och DynamoDB, vilket verifierar att datan laddas 
och sparas korrekt. SNS-notifieringar visas inte i trace-grafen eftersom de körs i bakgrunden
och fungerar som separata händelser via AWS SDK.

### IAM-roller & behörigheter
* EC2/EB-rollen:
  * s3:GetObject, dynamodb:PutItem, sns:Publish, xray:PutTraceSegments.
* CodePipeline-rollen:
  * elasticbeanstalk:UpdateEnviroment, s3:*Object, codebuild:StartBuild.
* CodeBuild-rollen:
  * Läsa/Skriva till S3.

---

# Testning
Test gjordes via Postman:
1. Generera presigned URL -> ladda upp CSV till S3.
2. Starta importen -> applikationen läser filen, skriver till DynamoDB och SNS.
3. Kontrollera status och loggar och se resultat i DynamoDB och SNS.
4. Spåra med X-Ray.
![Första postman blev 200 OK.png](image/F%C3%B6rsta%20postman%20blev%20200%20OK.png)
![200 OK på data.sv filen i psotman.png](image/200%20OK%20p%C3%A5%20data.sv%20filen%20i%20psotman.png)
![200 OK med imports.png](image/200%20OK%20med%20imports.png)


---
# Infrastruktur & CI/CD
* CodePipeline körs vid varje push till GitHub.
* CodeBuild bygger projektet.
* Elastic Beanstalk uppdaterar EC2 Instansen.
* X-Ray loggar händelser.
* IAM-roller garanterar säker kommunikation mellan tjänsterna.