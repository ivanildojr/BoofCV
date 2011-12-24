/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.factory.feature.describe;

import boofcv.abst.feature.describe.*;
import boofcv.abst.filter.blur.BlurFilter;
import boofcv.abst.filter.derivative.ImageGradient;
import boofcv.alg.feature.describe.DescribePointGaussian12;
import boofcv.alg.feature.describe.DescribePointSteerable2D;
import boofcv.alg.feature.describe.DescribePointSurf;
import boofcv.alg.feature.describe.brief.BriefDefinition_I32;
import boofcv.alg.feature.describe.brief.FactoryBriefDefinition;
import boofcv.alg.feature.orientation.OrientationIntegral;
import boofcv.alg.transform.ii.GIntegralImageOps;
import boofcv.factory.feature.orientation.FactoryOrientationAlgs;
import boofcv.factory.filter.blur.FactoryBlurFilter;
import boofcv.factory.filter.derivative.FactoryDerivative;
import boofcv.struct.image.ImageSingleBand;

import java.util.Random;


/**
 * Factory for creating implementations of {@link DescribeRegionPoint}.
 *
 * @author Peter Abeles
 */
public class FactoryDescribeRegionPoint {

	/**
	 * <p>
	 * Standard SURF descriptor configured to balance speed and descriptor stability. Invariant
	 * to illumination, orientation, and scale.
	 * </p>
	 *
	 * @param isOriented True for orientation invariant.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeRegionPoint<T> surf( boolean isOriented , Class<T> imageType) {
		OrientationIntegral<II> orientation = null;

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		if( isOriented )
			orientation = FactoryOrientationAlgs.average_ii(6, true, integralType);
//			orientation = FactoryOrientationAlgs.sliding_ii(42,Math.PI/3.0,6,true,integralType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.<II>surf(integralType);
		return new WrapDescribeSurf<T,II>( alg ,orientation);
	}

	/**
	 * <p>
	 * Modified SURF descriptor configured for optimal descriptor stability.  Runs slower
	 * than {@link #surf(boolean, Class)}, but produces more stable results.
	 * </p>
	 *
	 * @param isOriented True for orientation invariant.
	 * @param imageType Type of input image.
	 * @return SURF description extractor
	 */
	public static <T extends ImageSingleBand, II extends ImageSingleBand>
	DescribeRegionPoint<T> surfm(boolean isOriented, Class<T> imageType) {
		OrientationIntegral<II> orientation = null;

		Class<II> integralType = GIntegralImageOps.getIntegralType(imageType);

		if( isOriented )
//			orientation = FactoryOrientationAlgs.average_ii(6, true, integralType);
			orientation = FactoryOrientationAlgs.sliding_ii(42,Math.PI/3.0,6,true,integralType);

		DescribePointSurf<II> alg = FactoryDescribePointAlgs.<II>msurf(integralType);
		return new WrapDescribeSurf<T,II>( alg ,orientation);
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	DescribeRegionPoint<T> gaussian12( int radius ,Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointGaussian12<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian12(radius,imageType);

		return new WrapDescribeGaussian12<T,D>(steer,gradient,imageType,derivType);
	}

	public static <T extends ImageSingleBand, D extends ImageSingleBand>
	DescribeRegionPoint<T> steerableGaussian( int radius , boolean normalized ,
													Class<T> imageType , Class<D> derivType ) {

		ImageGradient<T,D> gradient = FactoryDerivative.sobel(imageType,derivType);
		DescribePointSteerable2D<T, ?> steer = FactoryDescribePointAlgs.steerableGaussian(normalized,-1,radius,imageType);

		return new WrapDescribeSteerable<T,D>(steer,gradient,imageType,derivType);
	}

	/**
	 * <p>
	 * The BRIEF descriptor is HORRIBLY inefficient when used through this interface.  This functionality is only
	 * provided for testing and validation purposes.
	 * </p>
	 *
	 * @param radius Region's radius.  Typical value is 16.
	 * @param numPoints Number of feature/points.  Typical value is 512.
	 * @param blurSigma Typical value is -1.
	 * @param blurRadius Typical value is 4.
	 * @param isFixed Is the orientation and scale fixed? true for original algorithm described in BRIEF paper.
	 * @param imageType Type of gray scale image it processes.
	 * @return BRIEF descriptor
	 */
	public static <T extends ImageSingleBand>
	DescribeRegionPoint<T> brief(int radius, int numPoints,
									   double blurSigma, int blurRadius,
									   boolean isFixed,
									   Class<T> imageType) {

		if( isFixed) {
			BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
			BriefDefinition_I32 definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

			return new WrapDescribeBrief<T>(FactoryDescribePointAlgs.brief(definition,filter));
		} else {
			BlurFilter<T> filter = FactoryBlurFilter.gaussian(imageType,blurSigma,blurRadius);
			BriefDefinition_I32 definition = FactoryBriefDefinition.gaussian2(new Random(123), radius, numPoints);

			return new WrapDescribeBriefSo<T>(FactoryDescribePointAlgs.briefso(definition, filter));
		}
	}

	/**
	 * Creates a region descriptor based on pixel intensity values alone.  A classic and fast to compute
	 * descriptor, but much less stable than more modern ones.
	 *
	 * @param regionWidth How wide the pixel region is.
	 * @param regionHeight How tall the pixel region is.
	 * @param imageType Type of image it will process.
	 * @return Pixel region descriptor
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageSingleBand>
	DescribeRegionPoint<T> pixel( int regionWidth , int regionHeight , Class<T> imageType ) {
		return new WrapDescribePixelRegion(FactoryDescribePointAlgs.pixelRegion(regionWidth,regionHeight,imageType));
	}

	/**
	 * Creates a region descriptor based on normalized pixel intensity values alone.  This descriptor
	 * is designed to be light invariance, but is still less stable than more modern ones.
	 *
	 * @param regionWidth How wide the pixel region is.
	 * @param regionHeight How tall the pixel region is.
	 * @param imageType Type of image it will process.
	 * @return Pixel region descriptor
	 */
	@SuppressWarnings({"unchecked"})
	public static <T extends ImageSingleBand>
	DescribeRegionPoint<T> pixelNCC( int regionWidth , int regionHeight , Class<T> imageType ) {
		return new WrapDescribePixelRegionNCC(FactoryDescribePointAlgs.pixelRegionNCC(regionWidth,regionHeight,imageType));
	}
}