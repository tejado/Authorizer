/*
*
* Copyright (c) 2003-2014 Rony Shapiro <ronys@users.sourceforge.net>.
* All rights reserved. Use of the code is allowed under the
* Artistic License 2.0 terms, as specified in the LICENSE file
* distributed with this code, or available from
* http://www.opensource.org/licenses/artistic-license-2.0.php
*/
#ifndef __UTIL_H
#define __UTIL_H

// Util.h
//-----------------------------------------------------------------------------

#include "PwsPlatform.h"

#include "typedefs.h"

extern void trashMemory(void *buffer, size_t length);
extern void burnStack(unsigned long len); // borrowed from libtomcrypt
#endif /* __UTIL_H */
//-----------------------------------------------------------------------------
// Local variables:
// mode: c++
// End:
