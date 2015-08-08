/*
* Copyright (c) 2003-2014 Rony Shapiro <ronys@users.sourceforge.net>.
* All rights reserved. Use of the code is allowed under the
* Artistic License 2.0 terms, as specified in the LICENSE file
* distributed with this code, or available from
* http://www.opensource.org/licenses/artistic-license-2.0.php
*/
/// \file Util.cpp
//-----------------------------------------------------------------------------

#include <cstring>

#include "PwsPlatform.h"
#include "Util.h"

//-----------------------------------------------------------------------------
//Overwrite the memory
// used to be a loop here, but this was deemed (1) overly paranoid
// (2) The wrong way to scrub DRAM memory
// see http://www.cs.auckland.ac.nz/~pgut001/pubs/secure_del.html
// and http://www.cypherpunks.to/~peter/usenix01.pdf

#ifdef _WIN32
#pragma optimize("",off)
#endif
void trashMemory(void *buffer, size_t length)
{
  ASSERT(buffer != NULL);
  // {kjp} no point in looping around doing nothing is there?
  if (length > 0) {
    std::memset(buffer, 0x55, length);
    std::memset(buffer, 0xAA, length);
    std::memset(buffer,    0, length);
  }
}
#ifdef _WIN32
#pragma optimize("",on)
#endif

/**
Burn some stack memory
@param len amount of stack to burn in bytes
*/
void burnStack(unsigned long len)
{
  unsigned char buf[32];
  trashMemory(buf, sizeof(buf));
  if (len > static_cast<unsigned long>(sizeof(buf)))
    burnStack(len - sizeof(buf));
}
