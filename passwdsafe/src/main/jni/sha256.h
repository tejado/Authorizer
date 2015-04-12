/*
* Copyright (c) 2003-2014 Rony Shapiro <ronys@users.sourceforge.net>.
* All rights reserved. Use of the code is allowed under the
* Artistic License 2.0 terms, as specified in the LICENSE file
* distributed with this code, or available from
* http://www.opensource.org/licenses/artistic-license-2.0.php
*/
// sha256.h
// SHA256 for PasswordSafe, based on LibTomCrypt by
// Tom St Denis, tomstdenis@iahu.ca, http://libtomcrypt.org
//-----------------------------------------------------------------------------
#ifndef __SHA256_H
#define __SHA256_H

#include "typedefs.h"
#include "PwsPlatform.h"

class SHA256
{
public:
  enum {HASHLEN = 32, BLOCKSIZE = 64};
  SHA256();
  ~SHA256();
  void Update(const unsigned char *in, size_t inlen);
  void Final(unsigned char digest[HASHLEN]);

private:
  ulong64 length;
  size_t curlen;
  ulong32 state[8];
  unsigned char buf[BLOCKSIZE];
};

#endif /* __SHA256_H */
//-----------------------------------------------------------------------------
// Local variables:
// mode: c++
// End:
