package sdk.utility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageTrimmer {
  public static BufferedImage trimImage(File imageFile) throws IOException {
    BufferedImage source = ImageIO.read(imageFile);

    int top = findImageTop(source);
    int bottom = findImageBottom(source);
    int left = findImageLeft(source, top, bottom);
    int right = findImageRight(source, top, bottom);

    int height = bottom - top + 1;
    int width = right - left + 1;

    return source.getSubimage(left, top, width, height);
  }

  private static int findImageRight(BufferedImage source, int top, int bottom) {
    for (int x = source.getWidth() - 1; x >= 0; x--) {
      for (int y = top; y <= bottom; y++) {
        if (!isTransparent(source, x, y)) {
          return x;
        }
      }
    }
    return 0;
  }

  private static int findImageLeft(BufferedImage source, int top, int bottom) {
    for (int x = 0; x < source.getWidth(); x++) {
      for (int y = top; y <= bottom; y++) {
        if (!isTransparent(source, x, y)) {
          return x;
        }
      }
    }
    return source.getWidth() - 1;
  }

  private static int findImageBottom(BufferedImage source) {
    for (int y = source.getHeight() - 1; y >= 0; y--) {
      for (int x = 0; x < source.getWidth(); x++) {
        if (!isTransparent(source, x, y)) {
          return y;
        }
      }
    }
    return 0;
  }

  private static int findImageTop(BufferedImage source) {
    for (int y = 0; y < source.getHeight(); y++) {
      for (int x = 0; x < source.getWidth(); x++) {
        if (!isTransparent(source, x, y)) {
          return y;
        }
      }
    }
    return source.getHeight() - 1;
  }

  private static boolean isTransparent(BufferedImage source, int x, int y) {
    return source.getRGB(x, y) >> 24 == 0x00;
  }
}
