#include "vsdk/toolkit/environment/background/FixedBackground.h"

FixedBackground::FixedBackground(Camera* camera, RGBAImageUncompressed* image)
    : image(image), camera(camera)
{
}

void FixedBackground::setImage(RGBAImageUncompressed* image)
{
    this->image = image;
}

RGBAImageUncompressed* FixedBackground::getImage() const
{
    return image;
}

ColorRgb FixedBackground::colorInDireccion(const Vector3Dd&)
{
    return ColorRgb();
}

Camera* FixedBackground::getCamera() const
{
    return camera;
}

const Class FixedBackground::CLASS = { "FixedBackground", &Background::CLASS };

const Class *
FixedBackground::getClass() const
{
    return &CLASS;
}
