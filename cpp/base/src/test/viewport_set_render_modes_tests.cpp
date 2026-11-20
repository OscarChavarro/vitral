#include <gtest/gtest.h>
#include "java/lang/String.h"
#include "java/util/ArrayList.txx"
#include "vsdk/toolkit/gui/KeyEvent.h"
#include "vsdk/toolkit/gui/viewport/Viewport.h"
#include "vsdk/toolkit/gui/viewport/ViewportSet.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetCommands.h"
#include "vsdk/toolkit/gui/viewport/ViewportSetInteractionTechniques.h"

/**
Exercises the render modes available in a `ViewportSet`, which depend on the
technology presenting it (i.e. no GPU mode over a 2D canvas).
*/
class ViewportSetRenderModesTest : public ::testing::Test {
protected:
    static java::ArrayList<int> cpuOnlyModes()
    {
        java::ArrayList<int> modes;
        modes.add(static_cast<int>(Viewport::RENDER_MODE_HIDDEN_LINES));
        modes.add(static_cast<int>(Viewport::RENDER_MODE_RAYTRACING));
        return modes;
    }
};

TEST_F(ViewportSetRenderModesTest,
       given_aNewSet_when_askingForModes_then_gpuAndRaytracingAreAvailable)
{
    // Arrange
    ViewportSet* set = ViewportSet::createStandardSet("test");

    // Act / Assert (C++ port: the constants are not odr-used, as they have no
    // out of class definition)
    EXPECT_TRUE(set->isRenderModeAvailable(Viewport::RENDER_MODE_Z_BUFFER));
    EXPECT_TRUE(set->isRenderModeAvailable(Viewport::RENDER_MODE_RAYTRACING));
    EXPECT_FALSE(set->isRenderModeAvailable(Viewport::RENDER_MODE_HIDDEN_LINES));
    delete set;
}

TEST_F(ViewportSetRenderModesTest,
       given_gpuViewports_when_onlyCpuModesAreAvailable_then_theyChangeToTheDefaultOne)
{
    // Arrange
    ViewportSet* set = ViewportSet::createStandardSet("test");
    set->getViewport(2)->setRenderMode(Viewport::RENDER_MODE_RAYTRACING);

    // Act
    set->setAvailableRenderModes(cpuOnlyModes());

    // Assert
    EXPECT_EQ(static_cast<int>(Viewport::RENDER_MODE_HIDDEN_LINES), set->getViewport(0)->getRenderMode());
    EXPECT_EQ(static_cast<int>(Viewport::RENDER_MODE_RAYTRACING), set->getViewport(2)->getRenderMode());
    delete set;
}

TEST_F(ViewportSetRenderModesTest,
       given_onlyCpuModes_when_addingAViewport_then_itDoesNotStartInGpuMode)
{
    // Arrange
    ViewportSet* set = ViewportSet::createStandardSet("test");
    set->setAvailableRenderModes(cpuOnlyModes());

    // Act
    set->addViewport(new Viewport());

    // Assert
    EXPECT_EQ(static_cast<int>(Viewport::RENDER_MODE_HIDDEN_LINES), set->getViewport(4)->getRenderMode());
    delete set;
}

TEST_F(ViewportSetRenderModesTest,
       given_onlyCpuModes_when_cyclingWithTheDotKey_then_gpuModeIsNeverSelected)
{
    // Arrange
    ViewportSet* set = ViewportSet::createStandardSet("test");
    set->setAvailableRenderModes(cpuOnlyModes());
    ViewportSetInteractionTechniques techniques(set);
    KeyEvent dot;
    dot.unicodeId = '.';
    Viewport* selected = set->getSelectedViewport();

    // Act
    techniques.processKeyPressedEvent(dot);
    int afterFirst = selected->getRenderMode();
    techniques.processKeyPressedEvent(dot);
    int afterSecond = selected->getRenderMode();

    // Assert
    EXPECT_EQ(static_cast<int>(Viewport::RENDER_MODE_RAYTRACING), afterFirst);
    EXPECT_EQ(static_cast<int>(Viewport::RENDER_MODE_HIDDEN_LINES), afterSecond);
    delete set;
}

TEST_F(ViewportSetRenderModesTest,
       given_onlyCpuModes_when_processingTheGpuCommand_then_itIsRejected)
{
    // Arrange
    ViewportSet* set = ViewportSet::createStandardSet("test");
    set->setAvailableRenderModes(cpuOnlyModes());
    ViewportSetInteractionTechniques techniques(set);

    // Act
    bool processed = techniques.processCommand(ViewportSetCommands::IDV_RENDER_MODE_GPU);

    // Assert
    EXPECT_FALSE(processed);
    EXPECT_EQ(static_cast<int>(Viewport::RENDER_MODE_HIDDEN_LINES), set->getSelectedViewport()->getRenderMode());
    delete set;
}

TEST_F(ViewportSetRenderModesTest,
       given_everyRenderMode_when_convertingToCommandAndBack_then_modeIsKept)
{
    const int modes[] = {Viewport::RENDER_MODE_Z_BUFFER,
                         Viewport::RENDER_MODE_RAYTRACING,
                         Viewport::RENDER_MODE_HIDDEN_LINES,
                         Viewport::RENDER_MODE_HIDDEN_SURFACES};
    for ( int mode : modes ) {
        // Act
        java::String command = Viewport::commandForRenderMode(mode);

        // Assert
        EXPECT_EQ(mode, Viewport::renderModeForCommand(command));
    }
}
