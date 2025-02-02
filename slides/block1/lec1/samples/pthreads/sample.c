#include <ctype.h>
#include <errno.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

#define handle_error(msg) \
  do { perror(msg); exit(1); } while (0)

struct thread_info {    /* Used as argument to thread_start() */
  pthread_t thread_id;  /* ID returned by pthread_create() */
  int       thread_num; /* Application-defined thread # */
};

static void * thread_A_start(void *arg) {
  struct thread_info *tinfo = arg;
  printf("Thread A id = %d: hello world!\n", tinfo->thread_num);
  return NULL;
}

static void * thread_B_start(void *arg) {
  struct thread_info *tinfo = arg;
  printf("Thread B id = %d: hello world!\n", tinfo->thread_num);
  return 245;
}

int main(int argc, char *argv[]) {
  int s;

  struct thread_info * tinfo_a = malloc(sizeof(struct thread_info));
  if (tinfo_a == NULL) handle_error("malloc");

  struct thread_info * tinfo_b = malloc(sizeof(struct thread_info));
  if (tinfo_b == NULL) handle_error("malloc");

  pthread_attr_t attr;
  s = pthread_attr_init(&attr);
  if (s != 0) handle_error("pthread_attr_init");

  tinfo_a->thread_num = 1;
  tinfo_b->thread_num = 2;

  s = pthread_create(&(tinfo_a->thread_id), &attr, &thread_A_start, tinfo_a);
  if (s != 0) handle_error("pthread_create");

  s = pthread_create(&(tinfo_b->thread_id), &attr, &thread_B_start, tinfo_b);
  if (s != 0) handle_error("pthread_create");

  s = pthread_attr_destroy(&attr);
  if (s != 0) handle_error("pthread_attr_destroy");

  void *res;
  s = pthread_join(tinfo_a->thread_id, &res);
  if (s != 0) handle_error("pthread_join");
  printf("Joined with thread A, result= %p\n", (char *) res);

  s = pthread_join(tinfo_b->thread_id, &res);
  if (s != 0) handle_error("pthread_join");
  printf("Joined with thread B, result= %p\n", (char *) res);

  return 0;
}