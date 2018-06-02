# EspertoWatchAndroid
BLE Android app to interface with Esperto Watch.

To the future software developers:

## Current State of Application

### User Accounts
The app is designed to facilitate user associated biometric data acquisition. Currently, users can create accounts 
(outside of facebook or google domain), and use those accounts to login. During the login process, users are requried 
to enter a variety of specific data, including first name, last name, password etc. This data is reformatted and passed 
to the AWS server for storage in the "Accounts" table. At this time, the API Gateway has not been implemented, and the 
app communicates with the tables through specific config files, and specifically constructed classes per table (SQL-ish data calls). 
If you are not familiar with AWS, I would recommend going through their tutorial/help pages for guidance. The Esperto Watch app
utilizes a Mobile Hub project which contains the Dyanmo DB tables. This Hub project hosts both the Android application, as well as 
the corresponding web application. 

The only authentication mechanism, is via the Accounts table. Users enter their username and passwords, and that username is used 
to query the accounts table (usernames must be unique). The response from the query is compared against the entered password, and 
will either grant access to the data summary page, or will pop up an error dialog. 

#### User Registration

After entering the required information (it will check whether each text box contains an appropriate input), the registration process
will redirect you to a BLE scanning page, where a phone is pictured center on the screen. Once the phone is selected - a cool 
animation will appear. Essentially once started, this activity scans available devices, and theoretically presents them on the UI for
selection. Right now, I'm pretty sure it just seeks the BLE address I was working with, and if present - presents an icon on the screen.
In the future this should be modified for a range of BLE addresses, and enable the display of multiple devices available. To associate
the user account to the BLE device, the user just selects the appropriate icon. Once selected, all of the user information is reformatted
and pushed to the Accounts table.


#### TODO:
- Associate scanning process with scanning animation
- Legitimize animation
- Back buttons
- Debugging
- Password encryption (utilize Amazon Cognito)
- Facebook/google logins (Cognito)
- Make registration page more attractive
- Implement goal alternatives
  - Currently only enables default setting (ie. if goals button is selected during registration, it should default to "default"
- Implement API Gateway to add a server intermediary/get rid of supportive SQL-ish classes (ie. UserAccounts)

### Summary Page

Once the user logs in, the app will immediately connect to the BLE address listed in the "Accounts" table. Theoretically, 
this would allow the real-time acquisition of data. However, as the utilized BLE device had difficulties sending data, 
the reformatting of acquired data has not been completed. After a device has been connected, the BLE service will notify 
the Summary page, listing any discovered services/characterisitics. Depending on the device, these characteristics should
be filtered based on their UUIDs - to note the ones that are sending desired data. If different characterisitcs are being utilized
to send the HR and step data, both should be saved for use later (ie. filter results). 

Theoretically, the past way would be to enable notifications for both data types, to eliminate the requirement to continually read
characterisitics - as that is a total pain. Currently, as a stand-in, random data is presented on the summary page in a basic format, 
if that user has logged in before (I manually configured it just to appear with "mmacmahon" I believe), else it remains blank. In 
the future the real time data values should be presented there, and should also be stored locally in a buffer before being passed
to the appropriate No-SQL table. If you switched options on the bottom navigation menu, it should switch between displays. There
is a display for HR data, and there is one for step count data. As there was no long term data, switching between these screens 
while logged in as "mmacmahon", pulled fake data from the respective AWS tables for display. A lot of work will have to go into 
these screens, with the dependency on actual "faked" data, over spans of the day, week, month... etc.


#### TODO:
- Fake data (daily, weekly, monthly)
- Bridge the gap between acquiring BLE data and sending it to AWS
- Make UI pretty
- Add additional graphs 
- Create a new activity/settings option to allow users to modify their account information (including goal setting)
- Catch SMS/phone events and send them via BLE to watch

### BLE

BLE is fairly straightforward when you get used to it - I highly recommend reviewing the Android specs/examples for BLE programming,
as well as reviewing the standard protocols, as BLE is arguably the most difficult aspect of the app for unfamiliar users.
 
  

