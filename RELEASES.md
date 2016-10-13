## Release and testing procedure
If ready to release a new version, apply the following steps:
* Update the versionname, if necessary/appropiate
* Tag the versioncode in git preceded by a 'v' e.g.: `v1`
* Build the release APK, upload it to the website and Play Store
* Upload the `app/build/outputs/mapping/release/mapping.txt` to Firebase

When continuing development:
* Directly increase the versioncode by 1 (and commit it!), to allow differentiation between testing and release versions
