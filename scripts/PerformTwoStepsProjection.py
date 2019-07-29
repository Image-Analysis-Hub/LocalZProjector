#@ Dataset input_img
#@OUTPUT Dataset ref_surface_output
#@OUTPUT Dataset local_proj_output
#@ OpService ops
#@ DatasetService ds

from net.imagej.axis import Axes
from net.imglib2.util import Intervals
from net.imglib2 import FinalDimensions
from fr.pasteur.iah.localzprojector.process import ReferenceSurfaceOp
from fr.pasteur.iah.localzprojector.process import ReferenceSurfaceParameters
from fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters import Method
from fr.pasteur.iah.localzprojector.process import ExtractSurfaceOp
from fr.pasteur.iah.localzprojector.process import ExtractSurfaceParameters
from fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters import ProjectionMethod



#------------------
# Parameters.
#------------------


# What channel to use to create the reference surface?
channel = 0

# Reference surface parameters.
params_ref_surface = ReferenceSurfaceParameters.create() \
				.method( Method.MAX_OF_STD ) \
				.zMin( 0 ) \
				.zMax( 100000 ) \
				.filterWindowSize( 21 ) \
				.binning( 4 ) \
				.gaussianPreFilter( 0.1 ) \
				.medianPostFilterHalfSize( 20 ) \
				.targetChannel( channel ) \
				.get()

# Local projection parameters.
params_proj = ExtractSurfaceParameters.create() \
				.zOffset( 0, 0 ) \
				.zOffset( 1, +8 ) \
				.deltaZ( 0, 3 ) \
				.deltaZ( 1, 3 ) \
				.projectionMethod( 0, ProjectionMethod.MIP ) \
				.projectionMethod( 1, ProjectionMethod.MIP ) \
				.get()


#------------------
# Functions.
#------------------

def get_axis(axis_type):
    return {
        'X': Axes.X,
        'Y': Axes.Y,
        'Z': Axes.Z,
        'TIME': Axes.TIME,
        'CHANNEL': Axes.CHANNEL,
    }.get(axis_type, Axes.Z)


def crop_along_one_axis(ops, data, intervals, axis_type):
    """Crop along a single axis using Views.
 
    Parameters
    ----------
    intervals : List with two values specifying the start and the end of the interval.
    axis_type : Along which axis to crop. Can be ["X", "Y", "Z", "TIME", "CHANNEL"]
    """
 
    axis = get_axis(axis_type)
    interval_start = [data.min(d) if d != data.dimensionIndex(axis) else intervals[0] for d in range(0, data.numDimensions())]
    interval_end = [data.max(d) if d != data.dimensionIndex(axis) else intervals[1] for d in range(0, data.numDimensions())]
 
    interval = interval_start + interval_end
    interval = Intervals.createMinMax(*interval)
 
    out = ops.run("transform.crop", data, interval, True)
    return out

#------------------
# Main
#------------------

#-----
# Step 1: we generate the reference surface.

# Get the desired channel.
main_channel = crop_along_one_axis( ops, input_img, [ channel, channel ], "CHANNEL")

# Create the op for reference surface extraction.
ref_op = ops.op( ReferenceSurfaceOp, main_channel, params_ref_surface )

# Execute reference surface extraction.
ref_surface = ref_op.calculate( main_channel )
# Show it.
ref_surface_output = ds.create( ref_surface )

#-----
# Step 2: we extract the local projection.

# Create output image. Empty yet.
x_axis = get_axis( "X" )
y_axis = get_axis( "Y" )
channel_axis = get_axis( "CHANNEL" )
new_dimensions = [ input_img.dimension(x_axis),  input_img.dimension(y_axis), input_img.dimension(channel_axis) ]
local_proj = ops.create().img( FinalDimensions.wrap( new_dimensions ), \
		input_img.getImgPlus().firstElement() )

# Create the op for local projection.
local_proj_op = ops.op( ExtractSurfaceOp, local_proj, input_img.getImgPlus(), ref_surface, params_proj )

# Execute it.
local_proj_op.compute( input_img.getImgPlus(), ref_surface, local_proj  )

# Show end results.
local_proj_output = ds.create( local_proj )



