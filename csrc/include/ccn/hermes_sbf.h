#include<stdio.h>
#include<stdint.h>
#include<stdlib.h>
#include<string.h>
#include<math.h>

#define SBF_MAX_HASH 10  /* The max number of hash functions*/
#define SBF_MAX_SIZE (1 << 28)  /* The size of bloom filter might be much larger in real use cases */
#define SBF_LOAD_FACTOR 25 /* m = 25n: This leads to roughly 0.0001 false positive rate  */

struct hermes_bloom_wire {
    uint8_t n_hash;   /* number of hash functions to employ */
    uint32_t seed;  /* can seed hashes differently */
    uint8_t ** blooms; /* 32M maximum per bloom for now */
    uint8_t cur_bloom; /* the current in-use bloom filter, it is rotating*/
    uint8_t bloom_num; /* the number of regular bloom filters used*/
    uint16_t swap_interval;  /* swapping interval, in minutes*/
    uint64_t  size; /* size of the bloom filter */
};


struct hermes_bloom_wire * sbf_create(uint32_t, uint8_t, uint16_t,  uint32_t);
void sbf_insert(struct hermes_bloom_wire *, const char *, uint16_t);
uint8_t sbf_check(struct hermes_bloom_wire *, const char *, uint16_t);
void sbf_swap(struct hermes_bloom_wire *);

