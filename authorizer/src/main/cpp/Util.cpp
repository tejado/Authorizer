#pragma clang diagnostic push
#pragma ide diagnostic ignored "readability-magic-numbers"
#pragma ide diagnostic ignored "cppcoreguidelines-avoid-magic-numbers"
/*
* Copyright (c) 2003-2014 Rony Shapiro <ronys@users.sourceforge.net>.
* All rights reserved. Use of the code is allowed under the
* Artistic License 2.0 terms, as specified in the LICENSE file
* distributed with this code, or available from
* http://www.opensource.org/licenses/artistic-license-2.0.php
*/
/// \file Util.cpp
//-----------------------------------------------------------------------------

#include <array>
#include <cstring>

#include "Util.h"

//-----------------------------------------------------------------------------
//Overwrite the memory
// used to be a loop here, but this was deemed (1) overly paranoid
// (2) The wrong way to scrub DRAM memory
// see http://www.cs.auckland.ac.nz/~pgut001/pubs/secure_del.html
// and http://www.cypherpunks.to/~peter/usenix01.pdf

[[gnu::noinline]] void trashMemory(void *buffer, size_t length)
{
  if (length > 0) {
    std::memset(buffer, 0x55, length);
    std::memset(buffer, 0xAA, length);
    std::memset(buffer, 0, length);
  }
}

/**
Burn some stack memory
@param len amount of stack to burn in bytes
*/
[[gnu::noinline]] void burnStack(size_t len)
{
  std::array<unsigned char, 32> buf{};
  trashMemory(buf.data(), buf.size());
  if (len > buf.size()) {
    burnStack(len - buf.size());
  }
}

#pragma clang diagnostic pop
