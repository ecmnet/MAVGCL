/****************************************************************************
Copyright 2006, Colorado School of Mines and others.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
****************************************************************************/
package com.comino.jfx.extensions;

import java.awt.*;
import java.awt.image.IndexColorModel;
import javax.swing.event.EventListenerList;
import static java.lang.Math.*;


/**
 * A color map converts a range of double values to colors.
 * For any double value, a color map
 * (1) transforms the value to an integer pixel in the range [0,255],
 * (2) maps this integer pixel to a color using an index color model.
 * <p>
 * The method {@link #getIndex(double)} performs step (1). For any
 * double value, that method
 * (1a) clips to a specified min-max range of values,
 * (1b) linearly translates and scales to [0.0,255.0], and
 * (1c) rounds to the nearest integer pixel in [0,255].
 * Extensions of this class may of course override this method to
 * implement alternative mappings.
 * <p>
 * A color map maintains a list of color map listeners, and notifies those
 * listeners whenever its mapping from values to colors has changed.
 * @author Dave Hale, Colorado School of Mines
 * @version 2009.07.06
 */
public class ColorMap {

  /**
   * Color model for grays from black to white.
   */
  public static final IndexColorModel GRAY = getGray();

  /**
   * Color model for red to blue like Matlab's jet color map.
   */
  public static final IndexColorModel JET = getJet();

  /**
   * Color model for red to blue like GMT's jet color map.
   */
  public static final IndexColorModel GMT_JET = getGmtJet();

  /**
   * Color model for hues from red to blue.
   */
  public static final IndexColorModel HUE = getHue();

  /**
   * Color model for hues from red to blue.
   */
  public static final IndexColorModel HUE_RED_TO_BLUE = getHueRedToBlue();

  /**
   * Color model for hues from blue to red.
   */
  public static final IndexColorModel HUE_BLUE_TO_RED = getHueBlueToRed();

  /**
   * Color model for eight complete cycles of hues.
   */
  public static final IndexColorModel PRISM = getPrism();

  /**
   * Color model for red to white to blue.
   */
  public static final IndexColorModel RED_WHITE_BLUE = getRedWhiteBlue();

  /**
   * Color model for blue to white to red.
   */
  public static final IndexColorModel BLUE_WHITE_RED = getBlueWhiteRed();

  /**
   * Color model for gray to yellow to red.
   */
  public static final IndexColorModel GRAY_YELLOW_RED = getGrayYellowRed();

  /**
   * Constructs a color map for values in [0,1].
   * The integers 0 and 255 must be valid pixels for the color model.
   * @param colorModel the index color model.
   */
  public ColorMap(IndexColorModel colorModel) {
    this(0.0,1.0,colorModel);
  }

  /**
   * Constructs a color map for specified values.
   * The integers 0 and 255 must be valid pixels for the color model.
   * @param vmin the minimum value.
   * @param vmax the maximum value.
   * @param colorModel the index color model.
   */
  public ColorMap(double vmin, double vmax, IndexColorModel colorModel) {
    _vmin = vmin;
    _vmax = vmax;
    _colorModel = colorModel;
    cacheColors();
  }

  /**
   * Constructs a color map for specified values and colors.
   * The default value range is [0.0,1.0].
   * @param vmin the minimum value.
   * @param vmax the maximum value.
   * @param c array[256] of colors.
   */
  public ColorMap(double vmin, double vmax, Color[] c) {
    this(vmin,vmax,getReds(c),getGreens(c),getBlues(c));
  }

  /**
   * Constructs a color map for specified values and colors. Red, green,
   * and blue components are bytes in the range [0,255], inclusive.
   * @param vmin the minimum value.
   * @param vmax the maximum value.
   * @param r array[256] of reds.
   * @param g array[256] of greens.
   * @param b array[256] of blues.
   */
  public ColorMap(double vmin, double vmax, byte[] r, byte[] g, byte[] b) {
    this(vmin,vmax,new IndexColorModel(8,256,r,g,b));
  }

  /**
   * Constructs a color map for specified values and colors. Red, green,
   * and blue components are floats in the range [0,1], inclusive.
   * @param vmin the minimum value.
   * @param vmax the maximum value.
   * @param r array[256] of reds.
   * @param g array[256] of greens.
   * @param b array[256] of blues.
   */
  public ColorMap(double vmin, double vmax, float[] r, float[] g, float[] b) {
    this(vmin,vmax,getBytes(r),getBytes(g),getBytes(b));
  }

  /**
   * Constructs a color map for a specified solid color.
   * @param c a color.
   */
  public ColorMap(Color c) {
    this(0.0,1.0,c);
  }

  /**
   * Constructs a color map for a specified solid color within a given [0,1]
   * range.
   * @param vmin the minimum value.
   * @param vmax the maximum value.
   * @param c a color.
   */
  public ColorMap(double vmin, double vmax, Color c) {
    this(vmin,vmax,makeSolidColors(c));
  }

  /**
   * Gets the minimum value in the range of mapped values.
   * @return the minimum value.
   */
  public double getMinValue() {
    return _vmin;
  }

  /**
   * Gets the maximum value in the range of mapped values.
   * @return the maximum value.
   */
  public double getMaxValue() {
    return _vmax;
  }

  /**
   * Gets the index color model used by this color map.
   * @return the index color model.
   */
  public IndexColorModel getColorModel() {
    return _colorModel;
  }

  /**
   * Gets the color corresponding to the specified value.
   * @param v the value to be mapped to a color.
   * @return the color.
   */
  public Color getColor(double v) {
    return _colors[getIndex(v)];
  }

  /**
   * Gets the index in the range [0,255] corresponding to the specified value.
   * @param v the value to be mapped to an index.
   * @return the index in the range [0,255].
   */
  public int getIndex(double v) {
    v = Math.max(_vmin,Math.min(_vmax,v));
    double s = (256.0-Math.ulp(256.0))/(_vmax-_vmin);
    return (int)((v-_vmin)*s);
  }

  /**
   * Maps an array of floats to a packed array of RGB float values in [0,1].
   * @param v the array of float values to be mapped to colors.
   * @return array[3*v.length] of packed RGB float values.
   */
  public float[] getRgbFloats(float[] v) {
    int nv = v.length;
    float[] rgb = new float[3*nv];
    float scale = 1.0f/255.0f;
    for (int i=0,iv=0; iv<nv; ++iv) {
      Color c = _colors[getIndex(v[iv])];
      rgb[i++] = scale*c.getRed();
      rgb[i++] = scale*c.getGreen();
      rgb[i++] = scale*c.getBlue();
    }
    return rgb;
  }

  /**
   * Maps an array of floats to a packed array of HSL float values in [0,1].
   * @param v the array of float values to be mapped to HSL values.
   * @return array[3*v.length] of packed HSL float values.
   */
  public float[] getHslFloats(float[] v) {
    float[] rgb = getRgbFloats(v);
    int nv = v.length;
    float[] hsl = new float[3*nv];
    float[] value = new float[3];
    for (int i=0,iv=0; iv<nv; ++iv) {
      value = rgbToHsl(rgb[i+0],rgb[i+1],rgb[i+2]);
      hsl[i++] = value[0];
      hsl[i++] = value[1];
      hsl[i++] = value[2];
    }
    return hsl;
  }

  /**
   * Maps color values to the CIE L*a*b* (CIELab) colorspace.
   * <p>
   * CIE L*a*b* is a non-linear color space specified by the "Commission
   * internationale de l'éclairage", or CIE (English: International
   * Commission of Illumination), and describes all colors visible to the
   * human eye. This colorspace defines color positions along three axes: one
   * axis being lightness (L*), one axis representing the position between
   * magenta and green (a*), and one axis representing the position between
   * yellow and blue (b*).
   * <p>
   * The design of this colorspace is to control mimic logarithmic response
   * the human eye, and when mapped into three-dimensional space the
   * perceptual difference can be estimated by the measuring the Euclidean
   * distance between points.
   * @param v the array of floats to be mapped to CIELab values.
   * @return array[3*v.length] of packaed CIELab values.
   */
  public float[] getCieLabFloats(float[] v) {
    int nv = v.length;
    float[] rgb = getRgbFloats(v);
    float[] lab = new float[3];
    float[] cielab = new float[3*nv];

    float r,g,b;
    float Ls,as,bs;
    float X,Y,Z;
    float XXn,YYn,ZZn;

    // CIE XYZ tristiumulus values of the reference white.
    float Xn = 0.95047f;
    float Yn = 1.00000f;
    float Zn = 1.08883f;

    for (int i=0,j=0,iv=0; iv<nv; ++iv) {
      r = rgb[j++]; g = rgb[j++]; b = rgb[j++];
      lab = rgbToCieLab(r,g,b);
      cielab[i++] = lab[0];
      cielab[i++] = lab[1];
      cielab[i++] = lab[2];
    }
    return cielab;
  }

  /**
   * Maps an RGB value into HSL colorspace.
   * <p>
   * Hue (H) is measured as an angle [0-360).
   * Saturation (S) and Lightness (L) are computed as decimal percent [0,1].
   * @param r the red color value in range [0,1].
   * @param g the green color value in range [0,1].
   * @param b the blue color value in range [0,1].
   * @return an array[3] containing the HSL values.
   */
  public static float[] rgbToHsl(float r, float g, float b) {
    float h,s,l;
    float[] hsl = new float[3];
    float min = min(min(r,g),b);
    float max = max(max(r,g),b);
    l = (max+min)/2f;
    if (max==min)
      h = s = 0.0f; // achromatic
    else {
      float diff = max - min;
      s = (l>0.5f) ? diff / (2f - max - min) : diff / (max + min);
      if (max==r)
        h = (g-b)/diff + ((g<b) ? 6.0f : 0.0f);
      else if (max==g)
        h = (b-r)/diff + 2.0f;
      else
        h = (r-g)/diff + 4.0f;

      h/=6.0f;
    }
    hsl[0] = h * 360;
    hsl[1] = s;
    hsl[2] = l;
    return hsl;
  }

  /**
   * Maps an HSL value into the RGB colorspace.
   * @param h the hue in range [0,360).
   * @param s the saturation in range [0,1].
   * @param l the lightness in range [0,1].
   * @return an array[3] containing mapped RGB values.
   */
  public static float[] hslToRgb(float h, float s, float l) {
    float r = 0, g = 0, b = 0;
    float c = (1 - Math.abs(2*l-1)) * s;
    float x = c * (1 - Math.abs((h/60.0f)%2-1));
    float m = l - c/2.0f;

         if (h>=  0 && h< 60) { r = c; g = x; b = 0;}
    else if (h>= 60 && h<120) { r = x; g = c; b = 0;}
    else if (h>=120 && h<180) { r = 0; g = c; b = x;}
    else if (h>=180 && h<240) { r = 0; g = x; b = c;}
    else if (h>=240 && h<300) { r = x; g = 0; b = c;}
    else                      { r = c; g = 0; b = x;}

    r += m;
    g += m;
    b += m;

    r = min(1.0f,max(0.0f,r));
    g = min(1.0f,max(0.0f,g));
    b = min(1.0f,max(0.0f,b));

    return new float[] {r,g,b};
  }

  /**
   * Converts an RGB value to the CIE L*a*b* colorspace.
   * @param rgb an array containing an RGB value.
   * @return an array[3] containing the CIE L*a*b* values.
   */
  public static float[] rgbToCieLab(float[] rgb) {
    float[] xyz = rgbToCieXyz(rgb);
    // CIE XYZ tristiumulus values of the reference white D65
    float Xn = 95.047f;
    float Yn = 100.000f;
    float Zn = 108.883f;
    xyz[0]/=Xn; xyz[1]/=Yn; xyz[2]/=Zn;

    return cieXyzToCieLab(xyz);
  }

  /**
   * Converts an RGB value to the CIE L*a*b* colorspace.
   * @param r the red color value [0,1].
   * @param g the green color value [0,1].
   * @param b the blue color value [0,1].
   * @return an array[3] of CIE L*a*b* values
   */
  public static float[] rgbToCieLab(float r, float g, float b) {
    return rgbToCieLab(new float[] { r, g, b });
  }

   /**
   * Converts a CIE L*a*b* color value to an RGB color value.
   * @param lab an array containing the CIE L*a*b* values.
   * @return an array[3] containing RGB values.
   */
  public static float[] cieLabToRgb(float[] lab) {
    float[] xyz = cieLabToCieXyz(lab);

    // CIE XYZ tristiumulus values of the reference white D65
    float Xn = 95.047f;
    float Yn = 100.000f;
    float Zn = 108.883f;
    xyz[0]*=Xn; xyz[1]*=Yn; xyz[2]*=Zn;

    float[] rgb = cieXyzToRgb(xyz);


    rgb[0] = min(1.0f,max(0.0f,rgb[0]));
    rgb[1] = min(1.0f,max(0.0f,rgb[1]));
    rgb[2] = min(1.0f,max(0.0f,rgb[2]));

    return rgb;
  }

  /**
   * Converts a CIE L*a*b* color value to an RGB color value.
   * @param Ls the CIE Lightness (L*) value.
   * @param as the CIE a* value.
   * @param bs the CIE b* value.
   * @return an array[3] containing RGB values.
   */
  public static float[] cieLabToRgb(float Ls, float as, float bs) {
    return cieLabToRgb(new float[] { Ls, as, bs });
  }

  /**
   * Sets the min-max range of values mapped to colors. Values outside this
   * range are clipped. The default range is [0.0,1.0].
   * @param vmin the minimum value.
   * @param vmax the maximum value.
   */
  public void setValueRange(double vmin, double vmax) {
    _vmin = vmin;
    _vmax = vmax;
  }

  /**
   * Sets the index color model for this color map.
   * @param colorModel the index color model.
   */
  public void setColorModel(IndexColorModel colorModel) {
    _colorModel = colorModel;
    cacheColors();
  }

  /**
   * Sets the index color model for this color map to a single color.
   * @param c a color.
   */
  public void setColorModel(Color c) {
    _colorModel = makeSolidColors(c);
    cacheColors();
  }


  /**
   * Gets a linear gray black-to-white color model.
   * @return the color model.
   */
  public static IndexColorModel getGray() {
    return getGray(0.0,1.0);
  }

  /**
   * Gets a linear gray color model for the specified gray levels. Gray
   * levels equal to 0.0 and 1.0 correspond to colors black and white,
   * respectively.
   * @param g0 the gray level corresponding to index value 0.
   * @param g255 the gray level corresponding to index value 255.
   * @return the color model.
   */
  public static IndexColorModel getGray(double g0, double g255) {
    return getGray(g0,g255,1.0);
  }

  /**
   * Gets a linear gray color model for the specified gray levels. Gray
   * levels equal to 0.0 and 1.0 correspond to colors black and white,
   * respectively.
   * @param g0 the gray level corresponding to index value 0.
   * @param g255 the gray level corresponding to index value 255.
   * @param alpha the opacity for all colors in this color model.
   * @return the color model.
   */
  public static IndexColorModel getGray(double g0, double g255, double alpha) {
    float a = (float)alpha;
    Color[] c = new Color[256];
    for (int i=0; i<256; ++i) {
      float g = (float)(g0+i*(g255-g0)/255.0);
      c[i] = new Color(g,g,g,a);
    }
    return makeIndexColorModel(c);
  }

  /**
   * Gets a red-to-blue color model like Matlab's jet color map.
   * @return the color model.
   */
  public static IndexColorModel getJet() {
    return getJet(1.0);
  }

  /**
   * Gets a red-to-blue color model like Matlab's jet color map.
   * @param alpha the opacity for all colors in this color model.
   * @return the color model.
   */
  public static IndexColorModel getJet(double alpha) {
    return makeIndexColorModel(getJetColors(alpha));
  }

  /**
   * Gets a red-to-blue color model like GMT's jet color map.
   * @return the color model.
   */
  public static IndexColorModel getGmtJet() {
    return getGmtJet(1.0);
  }

  /**
   * Gets a red-to-blue color model like GMT's jet color map.
   * @param alpha the opacity for all colors in this color model.
   * @return the color model.
   */
  public static IndexColorModel getGmtJet(double alpha) {
    return makeIndexColorModel(getGmtJetColors(alpha));
  }

  /**
   * Gets a color model with eight complete cycles of hues.
   * @return the color model.
   */
  public static IndexColorModel getPrism() {
    return getHue(0.0,8.0);
  }

  /**
   * Gets a red-to-blue linear hue color model.
   * @return the color model.
   */
  public static IndexColorModel getHue() {
    return getHue(0.0,0.67);
  }

  /**
   * Gets a red-to-blue linear hue color model.
   * @return the color model.
   */
  public static IndexColorModel getHueRedToBlue() {
    return getHue(0.0,0.67);
  }

  /**
   * Gets a blue-to-red linear hue color model.
   * @return the color model.
   */
  public static IndexColorModel getHueBlueToRed() {
    return getHue(0.67,0.0);
  }

  /**
   * Gets a linear hue color model for the specified hues. Hues equal to
   * 0.00, 0.33, and 0.67, and 1.00 correspond approximately to the colors
   * red, green, blue, and red, respectively.
   * @param h0 the hue corresponding to index value 0.
   * @param h255 the hue corresponding to index value 255.
   * @return the color model.
   */
  public static IndexColorModel getHue(double h0, double h255) {
    return getHue(h0,h255,1.0);
  }

  /**
   * Gets a linear hue color model for the specified hues and alpha.
   * Hues equal to 0.00, 0.33, and 0.67, and 1.00 correspond
   * approximately to the colors red, green, blue, and red, respectively.
   * @param h0 the hue corresponding to index value 0.
   * @param h255 the hue corresponding to index value 255.
   * @param alpha the opacity for all colors in this color model.
   * @return the color model.
   */
  public static IndexColorModel getHue(double h0, double h255, double alpha) {
    Color[] c = new Color[256];
    int a = (int)(0.5+max(0.0,min(1.0,alpha))*255);
    for (int i=0; i<256; ++i) {
      float h = (float)(h0+i*(h255-h0)/255.0);
      Color rgb = Color.getHSBColor(h,1.0f,1.0f);
      c[i] = new Color(rgb.getRed(),rgb.getGreen(),rgb.getBlue(),a);
    }
    return makeIndexColorModel(c);
  }

  /**
   * Gets a red-white-blue color model.
   * @return the color model.
   */
  public static IndexColorModel getRedWhiteBlue() {
    Color[] c = new Color[256];
    for (int i=0; i<256; ++i) {
      float x = (float)i/255.0f;
      if (x<0.5f) {
        float a = x/0.5f;
        c[i] = new Color(1.0f,a,a);
      } else {
        float a = (x-0.5f)/0.5f;
        c[i] = new Color(1.0f-a,1.0f-a,1.0f);
      }
    }
    return makeIndexColorModel(c);
  }

  /**
   * Gets a blue-white-red color model.
   * @return the color model.
   */
  public static IndexColorModel getBlueWhiteRed() {
    Color[] c = new Color[256];
    for (int i=0; i<256; ++i) {
      float x = (float)i/255.0f;
      if (x<0.5f) {
        float a = x/0.5f;
        c[i] = new Color(a,a,1.0f);
      } else {
        float a = (x-0.5f)/0.5f;
        c[i] = new Color(1.0f,1.0f-a,1.0f-a);
      }
    }
    return makeIndexColorModel(c);
  }

  /**
   * Gets the gray-yellow-red color model.
   * @return the color model.
   */
  public static IndexColorModel getGrayYellowRed() {
    Color[] c = new Color[256];
    for (int i=0; i<256; ++i) {
      float x = (float)i/255.0f;
      if (x<0.5f) {
        float y = 2.0f*x;
        c[i] = new Color(y,y,y);
      } else {
        float g = (x<0.67f)?1.0f:3.0f-3.0f*x;
        float b = 2.0f-2.0f*x;
        c[i] = new Color(1.0f,g,b);
      }
    }
    return makeIndexColorModel(c);
  }

  /**
   * Returns an index color model for the specified array of 256 colors.
   * @param c array[256] of colors.
   * @return the index color model.
   */
  public static IndexColorModel makeIndexColorModel(Color[] c) {
    if (hasAlpha(c)) {
      return new IndexColorModel(8,256,
        getReds(c),getGreens(c),getBlues(c),getAlphas(c));
    } else {
      return new IndexColorModel(8,256,
        getReds(c),getGreens(c),getBlues(c));
    }
  }

  /**
   * Returns an index color model for a single color.
   * @param c a color.
   * @return the index color model.
   */
  public static IndexColorModel makeSolidColors(Color c) {
    Color[] colors = new Color[256];
    for (int i=0; i<256; ++i)
      colors[i] = c;
    return makeIndexColorModel(colors);
  }

  /**
   * Returns an index color model with specified opacity (alpha).
   * @param icm an index color model from which to copy RGBs.
   * @param alpha opacity in the range [0.0,1.0].
   * @return the index color model with alpha.
   */
  public static IndexColorModel setAlpha(IndexColorModel icm, double alpha) {
    int bits = icm.getPixelSize();
    int size = icm.getMapSize();
    byte[] r = new byte[size];
    byte[] g = new byte[size];
    byte[] b = new byte[size];
    byte[] a = new byte[size];
    icm.getReds(r);
    icm.getGreens(g);
    icm.getBlues(b);
    byte ia = (byte)(255.0*alpha+0.5);
    for (int i=0; i<size; ++i)
      a[i] = ia;
    return new IndexColorModel(bits,size,r,g,b,a);
  }

  /**
   * Returns an index color model with specified opacities (alphas).
   * @param icm an index color model from which to copy RGBs.
   * @param alpha array of opacities in the range [0.0,1.0].
   * @return the index color model with alphas.
   */
  public static IndexColorModel setAlpha(IndexColorModel icm, float[] alpha) {
    int bits = icm.getPixelSize();
    int size = icm.getMapSize();
    byte[] r = new byte[size];
    byte[] g = new byte[size];
    byte[] b = new byte[size];
    byte[] a = new byte[size];
    icm.getReds(r);
    icm.getGreens(g);
    icm.getBlues(b);
    int n = min(size,alpha.length);
    for (int i=0; i<n; ++i)
      a[i] = (byte)(255.0f*alpha[i]+0.5f);
    return new IndexColorModel(bits,size,r,g,b,a);
  }

  ///////////////////////////////////////////////////////////////////////////
  // private

  private double _vmin = 0.0;
  private double _vmax = 1.0;
  private IndexColorModel _colorModel;
  private Color[] _colors = new Color[256];


  private void cacheColors() {
    for (int index=0; index<256; ++index)
      _colors[index] = new Color(_colorModel.getRGB(index));
  }

  private static byte[] getReds(Color[] color) {
    int n = color.length;
    byte[] r = new byte[n];
    for (int i=0; i<n; ++i)
      r[i] = (byte)color[i].getRed();
    return r;
  }

  private static byte[] getGreens(Color[] color) {
    int n = color.length;
    byte[] g = new byte[n];
    for (int i=0; i<n; ++i)
      g[i] = (byte)color[i].getGreen();
    return g;
  }

  private static byte[] getBlues(Color[] color) {
    int n = color.length;
    byte[] b = new byte[n];
    for (int i=0; i<n; ++i)
      b[i] = (byte)color[i].getBlue();
    return b;
  }

  private static byte[] getAlphas(Color[] color) {
    int n = color.length;
    byte[] b = new byte[n];
    for (int i=0; i<n; ++i)
      b[i] = (byte)color[i].getAlpha();
    return b;
  }

  private static boolean hasAlpha(Color[] color) {
    int n = color.length;
    for (int i=0; i<n; ++i)
      if (color[i].getAlpha()!=255)
        return true;
    return false;
  }

  private static byte[] getBytes(float[] f) {
    int n = f.length;
    byte[] b = new byte[n];
    for (int i=0; i<n; ++i)
      b[i] = (byte)(f[i]*255.0f+0.5f);
    return b;
  }

  private static Color[] addAlpha(Color[] color, double alpha) {
    int n = color.length;
    float a = (float)alpha;
    Color[] c = new Color[n];
    for (int i=0; i<n; ++i) {
      float r = color[i].getRed()/255.0f;
      float g = color[i].getGreen()/255.0f;
      float b = color[i].getBlue()/255.0f;
      c[i] = new Color(r,g,b,a);
    }
    return c;
  }

  private static Color[] addAlpha(Color[] color, float[] alpha) {
    int n = color.length;
    Color[] c = new Color[n];
    for (int i=0; i<n; ++i) {
      float r = color[i].getRed()/255.0f;
      float g = color[i].getGreen()/255.0f;
      float b = color[i].getBlue()/255.0f;
      float a = alpha[i];
      c[i] = new Color(r,g,b,a);
    }
    return c;
  }

  private static Color[] getJetColors(double alpha) {
    float a = (float)alpha;
    Color[] c = new Color[256];
    for (int i=0; i<256; ++i) {
      float x = (float)i/255.0f;
      if (x<0.125f) { // 0.0, 0.0, 0.5:1.0
        float y = x/0.125f;
        c[i] = new Color(0.0f,0.0f,0.5f+0.5f*y,a);
      } else if (x<0.375f) { // 0.0, 0.0:1.0, 1.0
        float y = (x-0.125f)/0.25f;
        c[i] = new Color(0.0f,y,1.0f,a);
      } else if (x<0.625f) { // 0.0:1.0, 1.0, 1.0:0.0
        float y = (x-0.375f)/0.25f;
        c[i] = new Color(y,1.0f,1.0f-y,a);
      } else if (x<0.875f) { // 1.0, 1.0:0.0, 0.0
        float y = (x-0.625f)/0.25f;
        c[i] = new Color(1.0f,1.0f-y,0.0f,a);
      } else { // 1.0:0.5, 0.0, 0.0
        float y = (x-0.875f)/0.125f;
        c[i] = new Color(1.0f-0.5f*y,0.0f,0.0f,a);
      }
    }
    c[0] = Color.BLACK;
    return c;
  }

  private static Color[] getGmtJetColors(double alpha) {
    float a = (float)alpha;
    Color[] c = new Color[256];
    for (int i=0; i<256; ++i) {
      float x = (float)i/255.0f;
      if (x<0.125f) { // R=0.0, G=0.0, B=0.5:1.0
        float y = x/0.125f;
        c[i] = new Color(0.0f,0.0f,0.5f+0.5f*y,a);
      } else if (x<0.375f) { // R=0.0, G=0.0:1.0, B=1.0
        float y = (x-0.125f)/0.25f;
        c[i] = new Color(0.0f,y,1.0f,a);
      } else if (x<0.625f) { // R=1.0, G=1.0, B=1.0:0.5
        float y = (x-0.375f)/0.25f;
        c[i] = new Color(1.0f,1.0f,1.0f-0.5f*y,a);
      } else if (x<0.875f) { // R=1.0, G=1.0:0.0, B=0.0
        float y = (x-0.625f)/0.25f;
        c[i] = new Color(1.0f,1.0f-y,0.0f,a);
      } else { // R=1.0:0.5, G=0.0, B=0.0
        float y = (x-0.875f)/0.125f;
        c[i] = new Color(1.0f-0.5f*y,0.0f,0.0f,a);
      }
    }
    return c;
  }

  private static float[] cieXyzToCieLab(float[] xyz) {
    float c = 0.008856f;

    for (int i=0; i<3; ++i)
      xyz[i] = (xyz[i] > c) ? (float)Math.pow(xyz[i],1.0f/3.0f)
                            : (7.787f*xyz[i])+(16.0f/116.0f);

    float Ls = 116.0f *  xyz[1] - 16.0f;   // L*
    float as = 500.0f * (xyz[0] - xyz[1]); // a*
    float bs = 200.0f * (xyz[1] - xyz[2]); // b*

    return new float[] { Ls, as, bs };
  }

  private static float[] cieLabToCieXyz(float[] lab) {
    float c = 0.008856f;

    float y = (lab[0] + 16.0f) / 116.0f;
    float x = (lab[1] / 500.0f) + y;
    float z = y - (lab[2] / 200.0f);

    float[] xyz = new float[] { x, y, z };

    for (int i=0; i<3; ++i)
      xyz[i] = (Math.pow(xyz[i],3) > c) ?
        (float)Math.pow(xyz[i],3) : (xyz[i]-16.0f/116.0f)/7.787f;

    return xyz;
  }

  private static float[] rgbToCieXyz(float[] rgb) {
    float[] xyz = new float[3];
    float c = 0.04045f;

    for (int i=0; i<3; ++i) {
      rgb[i] = (rgb[i] > c) ?
        (float)Math.pow((rgb[i]+0.055f)/1.055f,2.4f) : rgb[i] / 12.92f;
      rgb[i]*=100;
    }

    xyz[0] = rgb[0]*0.4124f + rgb[1]*0.3576f + rgb[2]*0.1805f;
    xyz[1] = rgb[0]*0.2126f + rgb[1]*0.7152f + rgb[2]*0.0722f;
    xyz[2] = rgb[0]*0.0193f + rgb[1]*0.1192f + rgb[2]*0.9505f;

    return xyz;
  }

  private static float[] cieXyzToRgb(float[] xyz) {
    xyz[0]/=100.0f; xyz[1]/=100.0f; xyz[2]/=100.0f;
    float[] rgb = new float[3];
    float c = 0.0031308f;

    rgb[0] = xyz[0]* 3.2406f + xyz[1]*-1.5372f+xyz[2]*-0.4986f;
    rgb[1] = xyz[0]*-0.9689f + xyz[1]* 1.8758f+xyz[2]* 0.0415f;
    rgb[2] = xyz[0]* 0.0557f + xyz[1]*-0.2040f+xyz[2]* 1.0570f;

    for (int i=0; i<3; ++i) {
      rgb[i] = (rgb[i] > c) ?
        1.055f*((float)Math.pow(rgb[i],1.0f/2.4f))-0.055f : rgb[i]*12.92f;
    }

    return rgb;
  }
}