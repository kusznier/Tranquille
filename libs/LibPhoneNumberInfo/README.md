**Disclaimer:** This is a free and open source project, but it relies on third-party web-services for some functions to work. This project is not affiliated with any third-party entities in any other sense.


# LibPhoneNumberInfo

A free and open source library that provides phone number info using a third-party crowdsourced phone number database (from some other proprietary app).

The library is not currently ready for public use (unstable API, not properly tested), but if you decide to use it, please do not abuse the third-party service.


## Available data

The following data is available in the main offline database:

* a phone number,
* a category (telemarketers, dept collectors, scam, etc.),
* a number of negative reviews,
* a number of positive reviews,
* a number of neutral reviews.

The main database may receive delta updates from third-party servers.

The "featured" database provides "names" (company names or short descriptions) for some (presumably) subset of numbers in the main database.

The third-party servers can be queried for a list of detailed user reviews for a specific phone number.
A detailed review contains:

* A rating: positive, negative or neutral.
* A category: each review may have a different one.
* A title and a comment: the actual description the user left for the number.


## Privacy

The only known possible data leaks are the following:

* Database update procedure leaks user's IP address to the update servers.  
  The request also includes current database version (base or updated).
* Online review requests leak user's IP address coupled with the phone number in question.

No other identifiable information is sent with the requests.


## Rationale

Some may find the original application (whose DB and servers are used) hard to trust because of its proprietary nature (and also the use of firebase analytics and the like).  
But since the database behind that application is crowdsourced, some may find it acceptable (in a moral sense) to use that database in a separate open source application.  
Also, this project is meant to be non-commercial. So, there's that.


## License

[AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html).  


## Contributing

Any contributions are very welcome.


## How to use

```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
```

```groovy
    dependencies {
        implementation 'com.gitlab.xynngh:LibPhoneNumberInfo:master-SNAPSHOT'
    }
```

[YetAnotherCallBlocker](https://gitlab.com/xynngh/YetAnotherCallBlocker) Android app uses this library, see its code for examples.
