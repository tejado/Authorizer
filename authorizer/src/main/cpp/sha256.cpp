#pragma clang diagnostic push
#pragma ide diagnostic ignored "readability-magic-numbers"
#pragma ide diagnostic ignored "cppcoreguidelines-avoid-magic-numbers"
/*
 * Copyright (c) 2003-2014 Rony Shapiro <ronys@users.sourceforge.net>.
 * Copyright (c) 2019 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
// sha256.cpp
// SHA256 for PasswordSafe, based on LibTomCrypt by
// Tom St Denis, tomstdenis@iahu.ca, http://libtomcrypt.org
// Rewritten for C++14 by Jeff Harris
//-----------------------------------------------------------------------------
#include <algorithm>
#include <cstring>
#include "sha256.h"

#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCDFAInspection"
inline static uint32_t load32H(unsigned const char* y)
{
  return (static_cast<uint32_t>(y[0] & 0xffU)<<24U) |
         (static_cast<uint32_t>(y[1] & 0xffU)<<16U) |
         (static_cast<uint32_t>(y[2] & 0xffU)<<8U)  |
         (static_cast<uint32_t>(y[3] & 0xffU));
}
#pragma clang diagnostic pop

inline static void store32H(uint32_t x, unsigned char* y)
{
  y[0] = static_cast<unsigned char>((x>>24U) & 0xffU);
  y[1] = static_cast<unsigned char>((x>>16U) & 0xffU);
  y[2] = static_cast<unsigned char>((x>>8U) & 0xffU);
  y[3] = static_cast<unsigned char>(x & 0xffU);
}

inline static void store64H(uint64_t x, unsigned char* y)
{
  y[0] = static_cast<unsigned char>((x>>56U) & 0xffU);
  y[1] = static_cast<unsigned char>((x>>48U) & 0xffU);
  y[2] = static_cast<unsigned char>((x>>40U) & 0xffU);
  y[3] = static_cast<unsigned char>((x>>32U) & 0xffU);
  y[4] = static_cast<unsigned char>((x>>24U) & 0xffU);
  y[5] = static_cast<unsigned char>((x>>16U) & 0xffU);
  y[6] = static_cast<unsigned char>((x>>8U) & 0xffU);
  y[7] = static_cast<unsigned char>(x & 0xffU);
}

inline static uint32_t RORc(uint32_t x, unsigned int y)
{
  return ((x & 0xFFFFFFFFU) >> (y & 31U)) |
         ((x << (32-(y & 31U))) & 0xFFFFFFFFU);
}

/* Various logical functions */
inline static uint32_t Ch(uint32_t x, uint32_t y, uint32_t z)
{
  return z ^ (x & (y ^ z));
}

inline static uint32_t Maj(uint32_t x, uint32_t y, uint32_t z)
{
  return (((x | y) & z) | (x & y));
}

inline static uint32_t S(uint32_t x, unsigned int n)
{
  return RORc(x, n);
}

inline static uint32_t R(uint32_t x, unsigned int n)
{
  return (x & 0xFFFFFFFFU) >> n;
}

inline static uint32_t Sigma0(uint32_t x)
{
  return S(x, 2) ^ S(x, 13) ^ S(x, 22);
}

inline static uint32_t Sigma1(uint32_t x)
{
  return S(x, 6) ^ S(x, 11) ^ S(x, 25);
}

inline static uint32_t Gamma0(uint32_t x)
{
  return S(x, 7) ^ S(x, 18) ^ R(x, 3);
}

inline static uint32_t Gamma1(uint32_t x)
{
  return S(x, 17) ^ S(x, 19) ^ R(x, 10);
}

inline static void RND(
        uint32_t a, uint32_t b, uint32_t c, uint32_t& d,
        uint32_t e, uint32_t f, uint32_t g, uint32_t& h,
        uint32_t w, uint32_t ki)
{
  uint32_t t0 = h + Sigma1(e) + Ch(e, f, g) + ki + w;
  uint32_t t1 = Sigma0(a) + Maj(a, b, c);
  d += t0;
  h = t0 + t1;
}


/**
 * Compress 512 bits from the buffer
 * @param buf The buffer being compressed
 */
inline void SHA256::compress(const unsigned char* buf)
{
  /* copy state into S */
  State S = itsState;

  std::array<uint32_t, 64> W; // NOLINT(cppcoreguidelines-pro-type-member-init,hicpp-member-init)

  // copy the state into 512-bits into W[0..15]
  for (size_t i = 0; i < 16; i++) {
    W[i] = load32H(buf + (4 * i));
  }

  // fill W[16..63]
  for (size_t i = 16; i < 64; i++) {
    W[i] = Gamma1(W[i - 2]) + W[i - 7] + Gamma0(W[i - 15]) + W[i - 16];
  }

  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[0],0x428a2f98U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[1],0x71374491U);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[2],0xb5c0fbcfU);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[3],0xe9b5dba5U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[4],0x3956c25bU);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[5],0x59f111f1U);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[6],0x923f82a4U);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[7],0xab1c5ed5U);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[8],0xd807aa98U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[9],0x12835b01U);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[10],0x243185beU);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[11],0x550c7dc3U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[12],0x72be5d74U);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[13],0x80deb1feU);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[14],0x9bdc06a7U);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[15],0xc19bf174U);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[16],0xe49b69c1U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[17],0xefbe4786U);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[18],0x0fc19dc6U);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[19],0x240ca1ccU);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[20],0x2de92c6fU);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[21],0x4a7484aaU);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[22],0x5cb0a9dcU);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[23],0x76f988daU);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[24],0x983e5152U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[25],0xa831c66dU);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[26],0xb00327c8U);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[27],0xbf597fc7U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[28],0xc6e00bf3U);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[29],0xd5a79147U);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[30],0x06ca6351U);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[31],0x14292967U);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[32],0x27b70a85U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[33],0x2e1b2138U);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[34],0x4d2c6dfcU);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[35],0x53380d13U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[36],0x650a7354U);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[37],0x766a0abbU);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[38],0x81c2c92eU);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[39],0x92722c85U);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[40],0xa2bfe8a1U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[41],0xa81a664bU);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[42],0xc24b8b70U);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[43],0xc76c51a3U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[44],0xd192e819U);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[45],0xd6990624U);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[46],0xf40e3585U);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[47],0x106aa070U);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[48],0x19a4c116U);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[49],0x1e376c08U);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[50],0x2748774cU);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[51],0x34b0bcb5U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[52],0x391c0cb3U);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[53],0x4ed8aa4aU);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[54],0x5b9cca4fU);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[55],0x682e6ff3U);
  RND(S[0],S[1],S[2],S[3],S[4],S[5],S[6],S[7],W[56],0x748f82eeU);
  RND(S[7],S[0],S[1],S[2],S[3],S[4],S[5],S[6],W[57],0x78a5636fU);
  RND(S[6],S[7],S[0],S[1],S[2],S[3],S[4],S[5],W[58],0x84c87814U);
  RND(S[5],S[6],S[7],S[0],S[1],S[2],S[3],S[4],W[59],0x8cc70208U);
  RND(S[4],S[5],S[6],S[7],S[0],S[1],S[2],S[3],W[60],0x90befffaU);
  RND(S[3],S[4],S[5],S[6],S[7],S[0],S[1],S[2],W[61],0xa4506cebU);
  RND(S[2],S[3],S[4],S[5],S[6],S[7],S[0],S[1],W[62],0xbef9a3f7U);
  RND(S[1],S[2],S[3],S[4],S[5],S[6],S[7],S[0],W[63],0xc67178f2U);

  // feedback
  for (size_t i = 0; i < itsState.size(); i++) {
    itsState[i] += S[i];
  }
}

/**
 * Process a block of memory though the hash
 * @param in The data to hash
 * @param inlen The length of the data (octets)
 */
void SHA256::update(const unsigned char* in, size_t inlen)
{
  while (inlen > 0) {
    if (itsCurlen == 0 && inlen >= BLOCKSIZE) {
      compress(in);
      itsLength += BLOCKSIZE * 8;
      in += BLOCKSIZE;
      inlen -= BLOCKSIZE;
    } else {
      size_t n = std::min(inlen, (BLOCKSIZE - itsCurlen));
      memcpy(itsBuf.data() + itsCurlen, in, n);
      itsCurlen += n;
      in += n;
      inlen -= n;
      if (itsCurlen == BLOCKSIZE) {
        compress(itsBuf.data());
        itsLength += BLOCKSIZE * 8;
        itsCurlen = 0;
      }
    }
  }
}

/**
 * Terminate the hash to get the digest
 * @param digest The destination of the hash (32 bytes)
 */
void SHA256::final(std::array<unsigned char, HASHLEN>& digest)
{
  // increase the length of the message
  itsLength += itsCurlen * 8;

  // append the '1' bit
  itsBuf[itsCurlen++] = static_cast<unsigned char>(0x80);

  // if the length is currently above 56 bytes we append zeros
  // then compress.  Then we can fall back to padding zeros and length
  // encoding like normal.
  if (itsCurlen > 56) {
    while (itsCurlen < BLOCKSIZE) {
      itsBuf[itsCurlen++] = 0;
    }
    compress(itsBuf.data());
    itsCurlen = 0;
  }

  // pad up to 56 bytes of zeroes
  while (itsCurlen < 56) {
    itsBuf[itsCurlen++] = 0;
  }

  // store length
  store64H(itsLength, itsBuf.data() + 56);
  compress(itsBuf.data());

  // copy output
  for (size_t i = 0; i < 8; i++) {
    store32H(itsState[i], digest.data() + (4 * i));
  }
}

#pragma clang diagnostic pop
