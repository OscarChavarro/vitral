if [ ! -d ./web ]; then
    mkdir ./web
fi
cd classes
jar cf ../web/app.jar *
cd ..
cp ../../../lib/vsdk.jar ./web
cd ./web
keytool -genkey -keystore keys -alias vitralsdk
jarsigner -keystore keys app.jar vitralsdk
jarsigner -keystore keys vsdk.jar vitralsdk
cp run.jnlp ./web
