# SaferDriver
This app provides two main core functionalities:
1. Sleeping detection and alarm by using Google Vision API. 
   Using camera to capture the face and detect the eye blink frequency. If the frequency is lower than the threshold we defined, 
   it can be recogizied as sleeping and send alarm voice.
2. Road Information notification.
   Using Google Plances API to detect the location and send request to Azure to get road information, like speed limit or emergeny.
   Than read the information to nofity the driver.
