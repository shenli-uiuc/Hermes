#include "hermes_sbf.h"

#define HERMES_SET_BIT(bloom, pos) ( bloom[pos / 8] |= (1 << (pos % 8)))
#define HERMES_CHECK_BIT(bloom, pos) ((bloom[pos / 8] & (1 << (pos % 8))) > 0)

/**
 * Parameters:
 * hash: the previous hash result. 
 */
static inline uint32_t jenkins_hash(uint32_t hash, const unsigned char *key, size_t len)
{
    uint32_t i;
    for(i = 0; i < len; ++i)
    {
        hash += key[i];
        hash += (hash << 10);
        hash ^= (hash >> 6);
    }
    hash += (hash << 3);
    hash ^= (hash >> 11);
    hash += (hash << 15);
    return hash;
}


struct hermes_bloom_wire * sbf_create(uint32_t estimated_members, uint8_t bloom_num, uint16_t swap_interval,  uint32_t seed){
    uint32_t m, n, h;
    uint8_t i;
    struct hermes_bloom_wire *hbw;
    
    n = estimated_members;
    
    //calculate bloom size
    m = n * SBF_LOAD_FACTOR;
    m = (m > SBF_MAX_SIZE) ? SBF_MAX_SIZE : m;
    
    //calculate number of hashes
    h = 0.7 * m / n;
    h = (h > SBF_MAX_HASH) ? (SBF_MAX_HASH) : (h);

    hbw = (struct hermes_bloom_wire *)calloc(1, sizeof(struct hermes_bloom_wire));
    hbw->seed = seed;
    hbw->size = m;
    hbw->n_hash = h;
    hbw->bloom_num = bloom_num;
    hbw->swap_interval = swap_interval;
    hbw->blooms = (uint8_t **)calloc(bloom_num, sizeof(uint8_t *));
    printf("%d, %d\n", sizeof(uint8_t *), sizeof(uint8_t));
    printf("before for %d\n", bloom_num);
    for(i = 0; i < bloom_num; ++i){
        //printf("before alloc %d\n", i);
        hbw->blooms[i] = (uint8_t *)calloc(hbw->size / 8, sizeof(uint8_t));
        printf("%d\n", hbw->blooms[i]);
        //printf("%d, %d, %d\n", i, bloom_num, hbw->size);
    }
    printf("!!!\n");
    hbw->cur_bloom = 0;
    printf("???\n");   
 
    return hbw;
}   

void sbf_destroy(struct hermes_bloom_wire ** phbw){
    struct hermes_bloom_wire * hbw = *phbw;
    int i;
    if(NULL == hbw)
        return;

    for( i = 0; i < hbw->bloom_num; ++i){
        free(hbw->blooms[i]);
    }
    free(hbw->blooms);
    free(hbw);
    *phbw = NULL;
}

void sbf_insert(struct hermes_bloom_wire * hbw, const unsigned char * key, uint16_t len){
    uint8_t i, j, cnt;
    uint32_t seed;
    seed = hbw->seed;
    for(i = 0 ; i < hbw->n_hash; ++i){
        seed = jenkins_hash(seed, key, len);
        for(j = 0; j < hbw->bloom_num; ++j){
            HERMES_SET_BIT(hbw->blooms[j], seed % hbw->size);
        }       
    }
}

uint8_t sbf_check(struct hermes_bloom_wire * hbw, const unsigned char * key, uint16_t len){
    uint8_t i, cur_bloom;
    uint32_t seed;
    seed = hbw->seed;
    cur_bloom = hbw->cur_bloom;
    for(i = 0 ; i < hbw->n_hash; ++i){
        seed = jenkins_hash(seed, key, len);
        if(!HERMES_CHECK_BIT(hbw->blooms[cur_bloom], seed % hbw->size))
            return 0; 
    }   
    return 1;
}

void sbf_swap(struct hermes_bloom_wire * hbw){
    uint8_t  old_bloom;
    // We cannot clear the bloom filter first, as there can be multiple threads
    old_bloom = hbw->cur_bloom;
    hbw->cur_bloom = (hbw->cur_bloom + 1) % hbw->bloom_num;
    memset(hbw->blooms[old_bloom], 0, sizeof(uint8_t) * hbw->size / 8);
}

static void print_status(struct hermes_bloom_wire * hbw, uint8_t keyID, const unsigned char * key){
    printf("Key %d: %s is %d\n", keyID, key, sbf_check(hbw, key, strlen(key)));
}

//swapping thread is not implemented yet
int main(){
    const unsigned char * k1, * k2, * k3, * k4, * k5, * k6;
    struct hermes_bloom_wire * hbw = sbf_create(1000, 3, 10, 0);
    printf("creation done!"); 
    k1 = "abcde";
    k2 = "abilkff";
    k3 = "12kjib";
    k4 = "adfadsfkjahdskfjdsgadsb";
    k5 = "adsk jaidfaidhf lasdf";
    k6 = "adfa dfad agafdg g";
    sbf_insert(hbw, k1, strlen(k1));
    print_status(hbw, 1, k1);
    print_status(hbw, 2, k2);
    sbf_insert(hbw, k2, strlen(k2));
    print_status(hbw, 2, k2);
    sbf_insert(hbw, k3, strlen(k3));
    sbf_insert(hbw, k4, strlen(k4));
    sbf_insert(hbw, k5, strlen(k5));
    print_status(hbw, 4, k4);
    print_status(hbw, 6, k5);
    sbf_swap(hbw);
    print_status(hbw, 4, k4);
    print_status(hbw, 6, k5);
    sbf_swap(hbw);
    sbf_swap(hbw);
    print_status(hbw, 4, k4);
    print_status(hbw, 6, k5);
    sbf_destroy(&hbw);
    printf("%d\n", hbw);
    return 0;
}
