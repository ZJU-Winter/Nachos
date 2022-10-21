## Members of Group
* Wentao Huang
* Siran Ma
* Jiayi Wang

## Implementation Description and Test
### Alarm
#### 1. Code Modified
* Set a priority queue(**"blockedThreadQueue"**) to store threads that are waiting until x wall-clock time. Priority between threads are decided by the set wake up time(present time + waitUntil time x).
* waitUntil(): store the thread and wake up time in **blockedThreadQueue**, let the thread sleep.
* timerInterrupt(): search for all threads in **blockedThreadQueue** whose wake up time is due, let the threads ready.
#### 2. Testing Cases
* waitUntil -100, -10, 0, 1000, 10000, 100000 clock time, check whether waiting periods are approximately same as wakeUntil time x.
#### 3. Contributions
* Jiayi Wang proposed the idea of setting a HashMap to store the waiting threads. Siran Ma simplified it by using a priority queue. Wentan Huang implemented the code. Testing and analyzing together.
### Join
#### 1. Code Modified
* set a hashmap(**"joinedThreads"**) for recording all joined threads' relationship. Key is the child thread, Value is the parent thread.
* join(): First, do 2 asserts to ensure(1)thread is not joining itself.(2)thread that is going to join has not been joined before. There won't exist more than 2 threads joining and waiting for the same thread. Then, store this new relationship in **joinedThreads**, let current thread sleep.
* finish(): When a thread finishes, check whether it is in **joinedThreads**. If yes, it means that there exists a parent thread waiting for this child thread to finish. Find the parent thread according to **joinedThreads**, let the parent thread ready, delete this used relationship in **joinedThreads**.
#### 2. Testing Cases
* Test Case 1: child thread is finished before join.
* Test Case 2: child thread is not finished before join.
* Test Case 3: thread call join on itself.
* Test Case 4: join is called more than once on a thread.
* Test Case 5: one thread can join with multiple other threads in succession.
* Test Case 6: same as Homework2.
#### 3. Contributions
* Todo
### Condition
#### 1. Code Modified
* set a queue(**"waitQueue"**) of waiting threads. set a lock(**"conditionLock"**) to protect this condition variable's data.
* sleep(): First release **conditionLock**. Add the thread to **waitQueue**. Let thread sleep. Last reaquire **conditionLock**.
* wake(): First acquire **conditionLock**. Pop the first thread in **waitQueue**. If the thread is added in Alarm's waiting queue but hasn't been waked by Alarm, call ThreadedKernel.alarm.cancel() to effectively placing it in the scheduler ready set immediately. Cancel the thread in Alarm's waiting queue at the same time. Otherwise, if the thread hasn't been added in Alarm's waiting queue, just placing it in the scheduler ready set.
* wakeAll(): While loop + wake().
* sleepFor(): 
#### 2. Testing Cases
#### 3. Contributions
* Todo
### Rendezvous
