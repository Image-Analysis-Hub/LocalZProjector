#@ Dataset input_img
#@ OpService ops
#@ DatasetService ds
#@ DisplayService display


from net.imagej import Dataset
from fr.pasteur.iah.localzprojector.process import LocalZProjectionOp
from fr.pasteur.iah.localzprojector.process import ReferenceSurfaceParameters
from fr.pasteur.iah.localzprojector.process.ReferenceSurfaceParameters import Method
from fr.pasteur.iah.localzprojector.process import ExtractSurfaceParameters
from fr.pasteur.iah.localzprojector.process.ExtractSurfaceParameters import ProjectionMethod



#------------------
# Parameters.
#------------------


# What channel to use to create the reference surface?
channel = 0

# Reference surface parameters.
#params_ref_surface = ReferenceSurfaceParameters.deserialize( param_file_1 )
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
# Execute.
#------------------

# Create op, specifying only input type and the two parameters.
lzp_op = ops.op( LocalZProjectionOp, Dataset, params_ref_surface, params_proj )

# Put the images to process in a list.
images_to_process = [ input_img, input_img ]

# Loop over each image.
for img in images_to_process:

	# Execute local Z projection.
	local_proj = lzp_op.calculate( img )

	# Display results.
	local_proj_output = ds.create( local_proj )
	local_proj_output.setName( 'LocalProjOf_' + img.getName() )
	display.createDisplay( local_proj_output )

