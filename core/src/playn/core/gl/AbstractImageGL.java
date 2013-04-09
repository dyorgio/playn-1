/**
 * Copyright 2012 The PlayN Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package playn.core.gl;

import playn.core.Asserts;
import playn.core.Image;
import playn.core.InternalTransform;
import playn.core.Pattern;

public abstract class AbstractImageGL implements Image {

  protected final GLContext ctx;

  /** The current count of references to this image. */
  protected int refs;

  /** Whether this image repeats in the x/y direction. */
  protected boolean repeatX, repeatY;

  /** Whether to generate mipmaps for this image. */
  protected boolean mipmapped;

  /**
   * Increments this image's reference count. Called by {@link ImageLayerGL} to let the image know
   * that it's part of the scene graph. Note that this reference counting mechanism only exists to
   * make more efficient use of texture memory. Images are also used by things like {@link Pattern}
   * which does not support reference counting, thus images must also provide some fallback
   * mechanism for releasing their texture when no longer needed (like in their finalizer).
   */
  public void reference() {
    refs++; // we still create our texture on demand
  }

  /**
   * Decrements this image's reference count. Called by {@link ImageLayerGL} to let the image know
   * that may no longer be part of the scene graph.
   */
  public void release() {
    Asserts.checkState(refs > 0, "Released an image with no references!");
    if (--refs == 0) {
      clearTexture();
    }
  }

  /**
   * Draws this image with the supplied transform in the specified target dimensions.
   */
  void draw(GLShader shader, InternalTransform xform, float dx, float dy, float dw, float dh,
            int tint) {
    int tex = ensureTexture();
    if (tex > 0) {
      float sl = x(), st = y();
      float sr = sl + (repeatX ? dw : width()), sb = st + (repeatY ? dh : height());
      float texWidth = texWidth(), texHeight = texHeight();
      addQuad(ctx.quadShader(shader).prepareTexture(tex, tint), xform, dx, dy, dx + dw, dy + dh,
              sl / texWidth, st / texHeight, sr / texWidth, sb / texHeight);
    }
  }

  /**
   * Draws this image with the supplied transform, and source and target dimensions.
   */
  void draw(GLShader shader, InternalTransform xform, float dx, float dy, float dw, float dh,
            float sx, float sy, float sw, float sh, int tint) {
    int tex = ensureTexture();
    if (tex > 0) {
      sx += x(); sy += y();
      float texWidth = texWidth(), texHeight = texHeight();
      addQuad(ctx.quadShader(shader).prepareTexture(tex, tint), xform, dx, dy, dx + dw, dy + dh,
              sx / texWidth, sy / texHeight, (sx + sw) / texWidth, (sy + sh) / texHeight);
    }
  }

  void addQuad(GLShader shader, InternalTransform xform, float dx, float dy, float dw, float dh,
               float sl, float st, float sr, float sb) {
    shader.addQuad(xform, dx, dy, dw, dh, sl, st, sr, sb);
  }

  @Override
  public boolean repeatX() {
    return repeatX;
  }

  @Override
  public boolean repeatY() {
    return repeatY;
  }

  @Override
  public void setRepeat(boolean repeatX, boolean repeatY) {
    if (repeatX != this.repeatX || repeatY != this.repeatY) {
      this.repeatX = repeatX;
      this.repeatY = repeatY;
      clearTexture();
    }
  }

  @Override
  public void setMipmapped (boolean mipmapped) {
    if (this.mipmapped != mipmapped) {
      this.mipmapped = mipmapped;
      clearTexture();
    }
  }

  @Override
  public Pattern toPattern() {
    // TODO: this will cause freakoutery when used in a canvas
    return new GLPattern() {
      public AbstractImageGL image() {
        return AbstractImageGL.this;
      }
    };
  }

  @Override
  public void getRgb(int startX, int startY, int width, int height, int[] rgbArray,
                     int offset, int scanSize) {
    throw new UnsupportedOperationException("Cannot getRgb on " + getClass().getName());
  }

  @Override
  public Image transform(BitmapTransformer xform) {
    throw new UnsupportedOperationException("Cannot transform " + getClass().getName());
  }

  protected AbstractImageGL(GLContext ctx) {
    this.ctx = ctx;
  }

  /**
   * The x offset into our source image at which this image's region starts.
   */
  protected float x() {
    return 0;
  }

  /**
   * The y offset into our source image at which this image's region starts.
   */
  protected float y() {
    return 0;
  }

  /**
   * Returns the width of our underlying texture image.
   */
  protected float texWidth() {
    return width();
  }

  /**
   * Returns the height of our underlying texture image.
   */
  protected float texHeight() {
    return height();
  }
}
