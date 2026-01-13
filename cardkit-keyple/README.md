<center>
    <h1>CardKit Keyple</h1>
<p>
CardKit Keyple is a modular Java library designed to simplify communication with smart cards and card readers using the Keyple framework. It provides a clean core API, reader abstractions, transaction handling, APDU utilities, and data models for Calypso-based cards.

This library is intended to be consumed as a single JAR (fat/uber JAR) installed locally via Maven.

</p>
</center>

![Java 8+](https://img.shields.io/badge/java-8%2B-blue)
![Static Badge](https://img.shields.io/badge/supported-windows-blue)
![Static Badge](https://img.shields.io/badge/supported-linux-orange)

## Previous

- PC/SC smart card service

## Installation

Download the latest version and run the following command once:

**linux**
```shell
mvn install:install-file \
  -Dfile=cardkit-keyple-0.1.0.jar \
  -DgroupId=com.idear.devices.card.cardkit \
  -DartifactId=cardkit-keyple \
  -Dversion=0.1.0 \
  -Dpackaging=jar
```

**windows powershell**
```shell
mvn install:install-file `
  -Dfile=cardkit-keyple-0.1.0.jar `
  -DgroupId=com.idear.devices.card.cardkit `
  -DartifactId=cardkit-keyple `
  -Dversion=0.1.0 `
  -Dpackaging=jar
```

**windows cmd**
```shell
mvn install:install-file -Dfile=cardkit-keyple-0.1.0.jar -DgroupId=com.idear.devices.card.cardkit -DartifactId=cardkit-keyple -Dversion=0.1.0 -Dpackaging=jar
```

Add the dependency to you `pom.xml`
```xml
<dependency>
    <groupId>com.idear.devices.card.cardkit</groupId>
    <artifactId>cardkit-keyple</artifactId>
    <version>0.1.0</version>
</dependency>
```


## Example

### Start readers and transaction manager
```java
import com.idear.devices.card.cardkit.keyple.KeypleCalypsoSamReader;
import com.idear.devices.card.cardkit.keyple.KeypleCardReader;
import com.idear.devices.card.cardkit.keyple.KeypleTransactionManager;

private KeypleTransactionManager ktm;

public static void main(String[] args) {
    // Create a new instance reader matching names, example ".*CARD READER.*"
    KeypleCardReader kcr = new KeypleCardReader(CARD_READER_NAME, AID_APPLICATION);
    KeypleCalypsoSamReader kcsr = new KeypleCalypsoSamReader(SAM_READER_NAME, LOCK_SECRET_SAM);

    // Start readers connection
    kcr.connect();
    kcsr.connect();
    
    // Start the transaction manager
    ktm = new KeypleTransactionManager(kcr, kcsr);
    // All transaction are implemented by the transaction manager, after declaring, you can execute transactions at will.
    // Add a lister to card events, CARD_PRESENT or CARD_ABSENT
    ktm.getCardEventList().add(this::onCardStatus);
    // Start pooling card present/absent
    ktm.startCardMonitor();
}
```

### Simple debit example

```java
import com.idear.devices.card.cardkit.core.io.transaction.CardStatus;
import org.eclipse.keypop.calypso.card.WriteAccessLevel;

// Debit params required
private final LocationCode locationCode = new LocationCode(0xAAAAAA);
private final Provider provider = Provider.CABLEBUS;
private final TransactionType transactionType = TransactionType.GENERAL_DEBIT;
private final int amount = 10_00;

// It is recommended to use an executor for dispatch event data
private final ExecutorService executorCardEvent = Executors.newSingleThreadExecutor();

public void onCardStatus(CardStatus cardStatus) {
    // Just work with card present on reader
    if (cardStatus.equals(CardStatus.CARD_ABSENT))
        return;
    
    

    // Use try-cath for a better exception control
    try {
        // Open a secure session and read all card data, all transaction required a previous session
        CalypsoCardCDMX calypsoCardCDMX = ktm.readCardData(WriteAccessLevel.DEBIT)
                .throwException() // throw the exception transaction result and abort all process
                .getData();
        
        // Save the transaction result and use as you want
        TransactionResult<TransactionDataEvent> transactionResult = ktm.debitCard(
                calypsoCardCDMX,
                transactionType.getValue(),
                locationCode.getValue(),
                provider.getValue(),
                0,
                amount
        ).throwException(); // throw the exception transaction result and abort all process
        
        log.info("Debit card success");
        executorCardEvent.submit(() -> saveEvent(transactionResult.getData()));
    } catch (CardKitException cardKitException) {
        log.warn("Debit card aborted", cardKitException);
    } catch (Throwable throwable) {
        log.error("Debit card fatal error", throwable);
    }
}
```