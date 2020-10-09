# CellFileAngles

CellFileAngles is a plugin for ImageJ that allows to identify cell files within plant tissues, and to measure
the angles of boundaries between adjacent cells and the medial axis of the cell file.

The method was developed in the context of the work of 
[Schaefer et al., 2017, "The preprophase band of microtubules controls the robustness of division orientation in plants", 
Science 536, pp. 186-189](https://science.sciencemag.org/content/356/6334/186.abstract).

# Installation

The plugin requires the [MorphoLibJ library](https://github.com/ijpb/MorphoLibJ).

To install the CellFileAngles plugin, simplyh [download the .jar file](https://github.com/ijpb/CellFileAngles/releases) of the latest version, 
and place it into the plugins folder of ImageJ.
This will create a new menu item located at: Plugins -> IJPB -> Cell File Angle.

# Usage

At least one label image, containing the labels of each cell identidied within the tissue, is required. 
Another image representing the original microscopy image can be used for displaying result overlay.

* Run the plugin by selecting "Plugins -> IJPB -> Cell File Angle"
* Fill in the different options: the tissue type (either in the drop-down list, or as free text), the side of the cell file with respect to the growth direction of the root
* It is possible to specifiy the type of output of the results: as a Table, as text in the log file, and / or as graphical overlay over the original image.
