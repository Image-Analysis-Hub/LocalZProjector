![LocalZProjectorLogo-512-text](src/main/resources/fr/pasteur/iah/localzprojector/gui/LocalZProjectorLogo-512-text.png)



# Local Z projector documentation.

Local Z Projector is an ImageJ2 plugin to perform local-Z projection of a 3D stack, possibly over time, possibly very large.

**Table of content.**

* [Citation.](#citation)
* [Installation.](#installation)
* [Aims.](#aims)
* [Example datasets.](#example-datasets)
  + [Dataset 1. Down-sampled *Drosophila* pupal notum image.](#dataset-1-down-sampled--drosophila--pupal-notum-image)
  + [Dataset 2. *Drosophila* pupal notum image.](#dataset-2--drosophila--pupal-notum-image)
  + [Dataset 3. Adult zebrafish brain image.](#dataset-3-adult-zebrafish-brain-image)
* [Example.](#example)
* [Getting the plugin.](#getting-the-plugin)
* [Usage.](#usage)
  + [The `Target image` panel.](#the--target-image--panel)
  + [The `Height-Map` panel.](#the--height-map--panel)
    - [How to determine parameter values?](#how-to-determine-parameter-values-)
      * [Target channel.](#target-channel)
      * [Binning](#binning)
      * [Method](#method)
        + [Max of mean.](#max-of-mean)
        + [Max of std](#max-of-std)
        + [Mean max on grid](#mean-max-on-grid)
        + [Std max on grid](#std-max-on-grid)
      * [Neighborhood size](#neighborhood-size)
      * [Z search min & max](#z-search-min---max)
      * [Gaussian pre-filter sigma](#gaussian-pre-filter-sigma)
      * [Median post-filter size.](#median-post-filter-size)
    - [The load, save and default buttons.](#the-load--save-and-default-buttons)
  + [The `Local projection` panel.](#the--local-projection--panel)
    - [Method.](#method)
    - [Offset.](#offset)
    - [DeltaZ.](#deltaz)
  + [The `Execute` panel.](#the--execute--panel)
* [Related work.](#related-work)
  + [DeProj.](#deproj)
  + [Other projection tools.](#other-projection-tools)

## Citation.

If you use this work for your Research, please cite the paper it is described in:

> __LocalZProjector and DeProj: a toolbox for local 2D projection and accurate morphometrics of large 3D microscopy images.__
>
> Sébastien Herbert, Léo Valon, Laure Mancini, Nicolas Dray, Paolo Caldarelli, Jérôme Gros, Elric Esposito, Spencer L. Shorte, Laure Bally-Cuif, Romain Levayer, Nathalie Aulner, Jean-Yves Tinevez
>
> BMC Biol 19, 136 (2021). doi: https://doi.org/10.1186/s12915-021-01037-w


## Installation.

LocalZProjector is a [Fiji](https://fiji.sc/) plugin and can be installed directly within the [Fiji updater](https://imagej.net/ImageJ_Updater).

In the `Manage update sites` window, check the `Local Z Projector` plugin. Click the `Close` button, then the `Apply changes` button. After the plugin is downloaded, restart Fiji. The plugin can then be launched from the _Plugins > Process > Local Z Projector_ menu item.

## Aims.

LZP performs projection of a surface of interest on a 2D plane from a 3D image. It is a simple tool that focuses on **usability** and is designed to be **adaptable** to many different use cases and image quality.

- It can work with 3D movies over time with multiple channels.
- It can work with images much larger than available RAM out of the box.
- It takes advantage of computers with multiple cores, and can be used in scripts.  

The local Z projection is based on first extracting a reference surface that maps the epithelial layer. The reference surface is represented by the **height-map**, made of one 2D plane per each time-point of the source image, that specifies for every (X, Y) position the Z position of the epithelial layer.  It is determined by applying a 2D filter on each plane of the 3D source image, chosen and configured to yield a strong response for the layer of interest. To speed-up computation and temper the effect of pixel noise, each 2D plane is first binned and filtered with a Gaussian. The height-map is then regularized using a median filter with a large window and rescaled to the original width and height.

![LZPPrinciple](docs/LZPPrinciple.png)

The height-map is then used to extract a projection from the 3D image. A fixed offset can be specified separately for each channel, and is used to collect intensity in planes above or below the reference surface.  Several planes, specified by a ∆z parameter, can be accumulated to generate a better projection, averaging the pixel values or taking the maximum value of these planes.

![ProjectionPrinciple](docs/ProjectionPrinciple.png)

## Example datasets.

Here are two examples available on Zenodo and that can be used to test the LocalZProjector plugin.
Both are of images of Drosophila pupa notum, captured on a confocal microscope by Léo Valon.

### Dataset 1. Down-sampled *Drosophila* pupal notum image.

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4449952.svg)](https://doi.org/10.5281/zenodo.4449952)

This dataset  also contains the parameters to generate the reference surface and local projection, also included. The two `*.localzprojector` files can be loaded directly in the plugin to load adequate parameters.
This is a __downsampled__ version of the dataset used to generate the figures 1, 2 and 3 in this paper, as well as the comparison in table 1. Because we downsampled it, the parameters and final image quality won't be identical that of the paper.

### Dataset 2. *Drosophila* pupal notum image.

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4449999.svg)](https://doi.org/10.5281/zenodo.4449999)

A much larger image, also of Drosophila pupa notum. Example parameters are also included.


### Dataset 3. Adult zebrafish brain image.

[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.4629231.svg)](https://doi.org/10.5281/zenodo.4629231)

An even larger image. An adult zebrafish brain, imaged on a laser-scanning confocal microscope.


## Example.

Input: a 3D stack, 2 channels, with an epithelium that resembles a smooth manifold in the green channel. The same channel is corrupted by dead cells for large values of Z, and by an auto-fluorescent cuticle for lower Z. The red channel contains cells, some of which are expressing a fluorescent reporter. It stains their nuclei, just below the epithelium.

![ExampleSource](docs/ExampleSource.png)

The Local Z Projector can generate a local 2D projection, from a few slices around a reference surface that follows the epithelium:

![ExampleProjection](docs/ExampleProjection.png)

It can also output the detected reference surface, in the shape of a height-map image that stores as pixel value, the Z plane of the reference surface in the source 3D image.

![ExampleHeightMap](docs/ExampleHeightMap.png)

## Getting the plugin.

The plugin is available via Fiji and can be downloaded using the Fiji updater.
To do so, you simply need to subscribe to the `LocalZProjector` update site on the common server:
![Update site](docs/UpdateSite.png)

## Usage.

After installation, the plugin is located in the `Plugins > Process` menu of Fiji.

Its UI is a made of a simple panel:

![LZPGUI](docs/LZPGUI.png)

### The `Target image` panel.

It just recapitulates properties of the image that will be used as source for the projection. 

### The `Height-Map` panel.

This panel sets the parameters used to detect the reference surface, or height-map.

#### How to determine parameter values?

The most important parameters are:

- the target channel - what channel has the information to determine the reference surface
- the method
- the neighborhood size
- the median post-filter.

The `default` button will reset parameters to values sensible to detect an epithelium stained for cell membranes.

##### Target channel.

This parameter sets in what channel is the structure to be used for reference surface detection. The index is 1-based (1 stands for the first channel).

##### Binning

Binning sets by how much the target channel is going to be binned. A value of 4 will merge 16 pixels (4x4) into 1, and shrink the size of data to process by as much. Because we take the mean of the 16 pixels when we bin, this also results in denoising the source image. High values of binning result in massive speedup.

A good starting points is to take the largest value that does not completely alter the global structure of the image you want to project. Typical starting values ranges from 4 to 8.

##### Method

Determines what filter to use for reference surface detection. Right now there are four methods:

###### Max of mean.

The best Z position is determined by looking for the maximum intensity along a Z column averaged in a NxN window of size given by the `Neighborhood size` parameter. 

This method is recommend for signals which are as homogeneous as possible along the tissue. It is as well usable in the case of very bright but fluctuating signal. We have found that it works also very well when there are no spurious structures in the source image beside the tissue image.

###### Max of std

This method uses  a standard deviation (std) filter instead of a mean filter.

It is suited to detect structure with ridges and strong edges, such as epithelia stained for their membrane (example pictured above). Because it is sensitive to contrast, it offers decent robustness against spurious structure with homogenous staining. This method is recommended for layers which contains contrasted signals. For instance junctional signals, that bright at the junctions, but dark at the cell centers.

###### Mean max on grid

This method works as the `Max of mean` except that the values are not calculated for all the pixels of a slice, but only on a sparse grid spaced by N/2 where N is given by the `Neighborhood size` parameter. In between the grid corners, the height-map values are obtained *via* linear interpolation. This method and the following one have been designed to speed-up the calculation, as we compute filter values on sparse positions. 

###### Std max on grid

The same, but with a standard deviation filter instead of a mean.

##### Neighborhood size

Sets the size of the filter configured by the `Method` parameter. The size is specified in pixels, regardless of the binning value.

This parameter should be chosen based on the how the tissue is imaged. It should be capped between two values. To start with, take values a bit larger than the structure you want to detect. For instance if you are using the `Max of std` method on an epithelium, starts with a size equal to 2 or 3 times the thickness of a membrane.

##### Z search min & max

Specifies values in case you want to restrict the search excluding some planes at the bottom or at the top of the stack.

Limiting the search range for Z can be beneficial to avoid taking into account (auto-) fluorescent signal coming from deep tissue and to speedup calculation. The Z limit will be applied to all time points.

##### Gaussian pre-filter sigma

Sets the standard deviation of the smoothing Gaussian filter to use before filtering. Only use non-zero values if your image is very noisy and you must use low values of the `Binning` parameter.

##### Median post-filter size.

The size of the median filter used to regularize the height-map.

Its size is specified in pixels. Because the median is applied on the binned image, the size needs to take the `Binning` parameter into account. For instance if the `Binning` is 4 and you need a median size of 100, just enter a value of 25.

Because of spurious structures that might appear far from the reference surface, the height-map can be locally aberrant. We use a median filter to correct for this. Use large values if you see that the resulting height-map is not smooth.

#### The load, save and default buttons.

The parameters you enter can be saved to a JSon file (a formatted text file) and retrieved later with these two buttons. The default button resets value to sensible defaults.

### The `Local projection` panel.

This panel configures how the projection is extracted once we have the reference surface. It is made a list of box, one for each channel in the source image. Each box specifies three parameters, separately for each channel, that specifies how to generate the projection for a channel.

After we have generated the reference surface, we can build a local projection from a smaller volume that follows this surface. The smaller volume is a layer that follows the reference surface, and of thickness given by the `DeltaZ` parameter.

#### Method.

This parameter specifies how to project the smaller volume:

- `MIP`: We take the maximum intensity projection.
- `Mean`: the take the mean along Z.
- `Collect `: we don't project on a single size, but instead output the smaller volume. If this method is selected for at least one channel, all the `Method` values in the other boxes are ignored.

#### Offset.

Specifies by how many slices to offset the reference surface for this channel. For instance if you want to grab the intensities in a layer 8 z-slices above the reference surface, just enter a value of +8.

#### DeltaZ.

Specifies the thickness of the smaller volume around the reference surface. The thickness of this volume is set to be ±∆z around the reference surface so a value of 2 results in incorporating 5 (2+1+2). A value of 0 takes only the reference surface plane.

### The `Execute` panel.

This is where you can run the projection process.

The `Preview` side on the left runs the process on the current time-point. The right part runs it on the whole movie (if any). In case you have a large file with many time-points, you can choose to save results for individual time-points.

## Related work.

### DeProj.

Check our MATLAB analysis tools called DeProj:

https://gitlab.pasteur.fr/iah-public/DeProj

It takes:

- the results of a segmentation on the projection image;
- the height-map;

and generates accurate morphological measurements of the tissue cells.

### Other projection tools.

There are several other tools to generate this kind of projections:

- Stack Focuser, an ImageJ plugin: https://imagej.nih.gov/ij/plugins/stack-focuser.html
- SurfCut, an Image macro: https://bmcbiol.biomedcentral.com/articles/10.1186/s12915-019-0657-1
- PreMosa, a standalone software: https://academic.oup.com/bioinformatics/article/33/16/2563/3104469
- Extended Depth of Field, an ImageJ plugin: http://bigwww.epfl.ch/demo/edf/
- Min. Cost Z Surface, an ImageJ plugin: https://imagej.net/Minimum_Cost_Z_surface_Projection
- FastSME and SME, MATLAB software for the extraction of smooth-manifold structures: https://openaccess.thecvf.com/content_cvpr_2018_workshops/w44/html/Basu_FastSME_Faster_and_CVPR_2018_paper.html
