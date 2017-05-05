This directory contains the control files for the debian package.  (Except this one which will not be copied).

The files and responsibilities are essentially as follows:

* conffiles - List of the configuration file needed/added by this debian packages
* control - package descriptor
* postinst - Any actions that need to be taken after the debian package has been ran can be put here
* postrm - This script is ran after removal of the debian package.  This should include any extra clean up that is required.
* prerm - This script is ran before the debian package is removed.
