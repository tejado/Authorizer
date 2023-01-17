/*
* Copyright (c) 2003-2014 Rony Shapiro <ronys@users.sourceforge.net>.
* Copyright (c) 2019 Jeff Harris <jefftharris@gmail.com>
* All rights reserved. Use of the code is allowed under the
* Artistic License 2.0 terms, as specified in the LICENSE file
* distributed with this code, or available from
* http://www.opensource.org/licenses/artistic-license-2.0.php
*/
// sha256.h
// SHA256 for PasswordSafe, based on LibTomCrypt by
// Tom St Denis, tomstdenis@iahu.ca, http://libtomcrypt.org
// Rewritten for C++14 by Jeff Harris
//-----------------------------------------------------------------------------
#ifndef __SHA256_H
#define __SHA256_H

#include <array>
#include <cstddef>
#include <cstdint>

class SHA256
{
public:
    static constexpr const size_t HASHLEN = 32;
    static constexpr const size_t BLOCKSIZE = 64;

    /// Constructor
    SHA256();

    /// Destructor
    ~SHA256();

    /// Process a block of memory though the hash
    void update(const unsigned char* in, size_t inlen);

    /// Terminate the hash to get the digest
    void final(std::array<unsigned char, HASHLEN>& digest);

private:

    using State = std::array<uint32_t, 8>;

    /// Compress 512 bits from the buffer
    void compress(const unsigned char* buf);

    /// Total number of bits hashed
    uint64_t itsLength;

    /// Hash state
    State itsState;

    /// Current length of itsBuf
    size_t itsCurlen;

    /// Buffered data that hasn't been added to the state yet
    std::array<unsigned char, BLOCKSIZE> itsBuf;
};

/**
 * Constructor
 */
inline SHA256::SHA256() :
        itsLength(0),
        itsState{0x6A09E667U, 0xBB67AE85U, 0x3C6EF372U, 0xA54FF53AU,
                 0x510E527FU, 0x9B05688CU, 0x1F83D9ABU, 0x5BE0CD19U},
        itsCurlen(0)
{
}

/**
 * Destructor
 */
inline SHA256::~SHA256() = default;

#endif /* __SHA256_H */
