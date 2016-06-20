# CuriousMe - AR for Android

CuriousMe is an augmented reality application developed by [Xhensila Doda](https://github.com/xhensiladoda) and [Davide Abati](https://github.com/DavideA) as midterm project for [MuMet](http://www.mastermumet.unimore.it/). It is basically composed of two building blocks:
* a web server, where the user can couple target images with YouTube videos, and store the combination;
* an Android application, that can recognize such target images once acquired by the device camera, and play the corresponding video in overlap.
Actually, this also relies on the [Vuforia SDK](https://developer.vuforia.com/downloads/sdk) and [Vuforia Web Services](https://developer.vuforia.com/library/articles/Solution/How-To-Use-the-Vuforia-Web-Services-API) to work, but everything will be clear in a minute!

By now, you can watch a fancy video of CuriousMe.
### How does it work?
The whole architecture is explained by this image:
![curiousme-architecture](http://www.davideabati.com/resources/curiousmearchitecture.png "CuriousMe architecture")

1 - User just publishes a new image/video combination on the web application, and the information gets forwarded to the Vuforia Cloud, as an image with textual metadata (the video url);

2 - The Vuforia SDK living in the Android Application queries the cloud web services about the presence of a target image in the acquired frame;

3 - If a target is present, its metadata are sent to the mobile application, containing the video URL;

4 - The mobile application opens a connection to the CuriousMe server, that acts as a proxy to YouTube (this is necessary because Android only supports HLS streams, so a conversion is needed);

5 - The video reaches the mobile phone, and is rendered upon the target image within the acquired frame.

### Getting started
Unfortunately, the web application is not available yet :(... but we're working on its release!
Anyway, you can still try the mobile application by manually uploading data in the Vuforia Cloud. Here's how it works:
* Create a Vuforia developer account [here](https://developer.vuforia.com/license-manager).
* In your Target Manager, create a cloud database and load your first target image, also providing a text file as metadata, containing the HLS url of the stream you want to couple;
* Checkout this repository in a local folder, and open it with [Android Studio](https://developer.android.com/studio/index.html) (integration with different IDEs is possible, but definitely NOT recommended);
* In your Vuforia Developer Portal, find out your client access keys for your database, and put them in app/src/main/java/com/mumet/abatidoda/curiousme/VideoPlayback/app/VideoPlayback/VideoPlayback.java, lines 112-113:
```java
private static final String kAccessKey = "INSERT ACCESS KEY HERE";
private static final String kSecretKey = "INSERT SECRET KEY HERE";
```
* Install the application on a device, open it and try to frame your target! :)
