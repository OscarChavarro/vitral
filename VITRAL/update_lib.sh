./cleanall.sh
cd vitral
./compile.sh
cd ..
cd stage034_Camera/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
cd stage042_CameraSwing/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
cd stage050_Image/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
cd stage052_ImageSwing/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
cd stage090_Raytracing/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
cd stage144_Mesh/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
cd stage666_SceneEditor/lib
ln -s ../../vitral/lib/vitral.jar .
cd ../..
