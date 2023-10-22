# Distributed Broadcast Algorithms

## Overview
This project was completed as part of EPFL's Distributed Algorithms course. The goal of the project is to implement the following building-blocks necessary for a decentralized system:
  - Perfect Links,
  - Best-effort Broadcast,
  - Uniform Reliable Broadcast,
  - FIFO Broadcast,
  - Localized Causal Broadcast

Note: the implementation takes into account that **messages** exchanged between processes **may be dropped, delayed or reordered by the network**. The execution of processes may be paused for an arbitrary amount of time and resumed later. Processes may also fail by crashing at arbitrary points of their execution.

# Project Requirements

## Basics

### Messages
Inter-process point-to-point messages (at the low level) must be carried exclusively by UDP packets in their most basic form, not utilizing any additional features (e.g., any form of feedback about packet delivery) provided by the network stack, the operating system or external libraries. Everything must be implemented on top of these low-level point to point messages.

The application messages (i.e., those broadcast by processes) are numbered sequentially at each process, starting from `1`. Thus, each process broadcasts messages `1` to `m`. By default, the payload carried by an application message is only the sequence number of that message. Though the payload is known in advance, your implementation should not utilize this information. In other words, your implementation should be agnostic to the contents of the payload. For example, your implementation should work correctly if the payload is arbitrary text instead of sequential numbers. In addition, your implementation should not rely on the fact that the total number of messages (to be broadcast) is known in advance, i.e., your implementation should work correctly if the number of messages is infinite.

### Template structure
We provide you a template for both C/C++ and Java, which you should use in your project. The template has a certain structure that is explained below:

#### For Java:
```sh
.
├── bin
│   ├── deploy
│   │   └── README
│   ├── logs
│   │   └── README
│   └── README
├── build.sh
├── cleanup.sh
├── pom.xml
├── run.sh
└── src
    └── main
        └── java
            └── cs451
                └── ...
```

### Interface
The templates provided come with a command line interface (CLI) that you should use in your deliverables. The implementation for the CLI is given to you for convenience. It is only for guidance, and you should not consider it complete by any means. You are encouraged to make any modifications to it, as long as it complies to the specification. You are also encouraged to test your code with different test cases and to stress test its performance in whatever way suits you.

The supported arguments are:
```sh
./run.sh --id ID --hosts HOSTS --output OUTPUT CONFIG
```

Where:
  - `ID` specifies the unique identifier of the process. In a system of `n` processes, the identifiers are `1`...`n`.
  - `HOSTS` specifies the path to a file that contains the information about every process in the system, i.e., it describes the system membership. The file contains as many lines as processes in the system. A process identity consists of a numerical process identifier, the IP address or name of the process and the port number on which the process is listening for incoming messages. The entries of each process identity are separated by white space character. The following is an example of the contents of a `HOSTS` file for a system of 5 processes:
  ```
1 localhost 11001
2 localhost 11002
3 localhost 11003
4 localhost 11004
5 localhost 11005
  ```
  **Note**: The processes should listen for incoming messages in the port range `11000` to `11999` inclusive. Each process should use only 1 port.

  - `OUTPUT` specifies the path to a text file where a process stores its output. The text file contains a log of events.  Each event is represented by one line of the output file, terminated by a Unix-style line break `\n`. There are two types of events to be logged:
    -  broadcast of application message, using the format `b`*`seq_nr`*,
  where `seq_nr` is the sequence number of the message.
    - delivery of application message, using the format `d`*`sender`* *`seq_nr`*, where *`sender`* is the number of the process that broadcast the message and *`seq_nr`* is the sequence number of the message (as numbered by the broadcasting process).

An example of the content of an output file:
```
b 1
b 2
b 3
d 2 1
d 4 2
b 4
```

- `CONFIG` specifies the path to a file that contains additional information for the experimented abstraction (e.g. how many message to broadcast).

A process that receives a `SIGTERM` or `SIGINT` signal must immediately stop its execution with the exception of writing to an output log file (see below). In particular, it must not send or handle any received network packets. This is used to simulate process crashes. You can assume that at most a minority (e.g., 1 out of 3; 2 out of 5; 4 out of 10, ...) processes may crash in one execution.

**Note:** The most straight-forward way of logging the output is to append a line to the output file on every broadcast or delivery event. However, this may harm the performance of the implementation. You might consider more sophisticated logging approaches, such as storing all logs in memory and write them to a file only when the `SIGINT` or `SIGTERM` signal is received. Also note that even a crashed process needs to output the sequence of events that occurred before the crash. You can assume that a process crash will be simulated only by the `SIGINT` or `SIGTERM` signals. Remember that writing to files is the only action we allow a process to do after receiving a `SIGINT` or `SIGTERM` signal.

### Compilation
All submitted implementations will be tested using Ubuntu 18.04 running on a 64-bit architecture.
These are the specific versions of toolchains where you project will be tested upon:
  - gcc (Ubuntu 7.5.0-3ubuntu1~18.04) 7.5.0
  - g++ (Ubuntu 7.5.0-3ubuntu1~18.04) 7.5.0
  - cmake version 3.10.2
  - OpenJDK Runtime Environment (build 11.0.8+10-post-Ubuntu-0ubuntu118.04.1)
  - Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)

All submitted files are to be placed in one zip file, in the same structure as the provided templates. Make sure that the top-level of the zip file is not a directory that contains the template (along with your source code inside `src`), but the template itself.

You are **strongly encouraged** to test the compilation of your code in the virtualbox [VM](https://github.com/LPD-EPFL/CS451-2021-project/blob/master/VM.txt) provided to you. **Submissions that fail to compile will NOT be considered for grading. Similarly, submissions that fail to produce any output files or produce faulty output files (e.g., empty files) will NOT be graded.**

**Detailed instructions for submitting your project will be released soon.**

### Execution
We define several details for each algorithms below.

#### Perfect Links application
  - The `CONFIG` command-line argument for this algorithm consists of a file that contains two integers `m i` in its first line. `m` defines how many messages each process should send. `i` is the index of the process that should receive the messages.
  Note that all processes, apart from `i`, send `m` messages each to process `i`.
  - Even though messages are not being broadcast, processes that send messages log them using the format `b`*`seq_nr`*.
  - Similarly, process `i` logs the messages using the format `d`*`sender`* *`seq_nr`*.
  - The following example builds and starts 3 processes (run from within the `template_cpp` or the `template_java` directory):
```sh
# Build the application:
./build.sh

# In first terminal window:
./run.sh --id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/perfect-links.config

# In second terminal window:
./run.sh --id 2 --hosts ../example/hosts --output ../example/output/2.output ../example/configs/perfect-links.config

# In third terminal window:
./run.sh --id 3 --hosts ../example/hosts --output ../example/output/3.output ../example/configs/perfect-links.config

# Wait enough time for all processes to finish processing messages.
# Type Ctrl-C in every terminal window to create the output files.
# Of course, you will NOT find any output files after running this because there is nothing implemented now!
```

#### FIFO Broadcast application
  - You must implement this on top of uniform reliable broadcast (URB).
  - The `CONFIG` command-line argument for this algorithm consists of a file that contains an integer `m` in its first line. `m` defines how many messages each process should broadcast.
  - The following example builds and starts 3 processes (run from within the `template_cpp` or the `template_java` directory):
```sh
# Build the application:
./build.sh

# In first terminal window:
./run.sh --id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/fifo-broadcast.config

# In second terminal window:
./run.sh --id 2 --hosts ../example/hosts --output ../example/output/2.output ../example/configs/fifo-broadcast.config

# In third terminal window:
./run.sh --id 3 --hosts ../example/hosts --output ../example/output/3.output ../example/configs/fifo-broadcast.config

# Wait enough time for all processes to finish processing messages.
# Type Ctrl-C in every terminal window to create the output files.
```

#### Localized Causal Broadcast
  - You must implement this on top of uniform reliable broadcast (URB).
  - The `CONFIG` command-line argument for this algorithm consists of a file that contains an integer `m` in its first line. `m` defines how many messages each process should broadcast.
  - For a system of `n` processes, there are `n` more lines in the `CONFIG` file. Each line `i` corresponds to process `i`, and such a line indicates the identities of other processes which can affect process `i`. See the example below.
  - The FIFO property still needs to be maintained by localized causal broadcast. That is, messages broadcast by the same process must not be delivered in a different order then they were broadcast.
  - The output format for localized causal broadcast remains the same as before.
  Example of `CONFIG` file for a system of `5` processes, where each one broadcasts `m` messages:
```
m
1 4 5
2 1
3 1 2
4
5 3 4
```
*Note*: Lines should end in `\n`, and numbers are separated by white-space characters.

In this example we specify that process `1` is affected by messages broadcast by processes `4` and `5`. Similarly, we specify that process `2` is only affected by process `1`. Process `4` is not affected by any other processes. Process `5` is affected by processes `3` and `4`.

We say that a process `x` is affected by a process `z` if all the messages which process `z` broadcasts and which process `x` delivers become dependencies for all future messages broadcast by process `x`. We call these dependencies *localized*. If a process is not affected by any other process, messages it broadcasts only depend on its previously broadcast messages (due to the FIFO property).

*Note*:  In the default causal broadcast (this algorithm will be discussed in one of the lectures) each process affects `all` processes. In this algorithm we can selectively define which process affects some other process.
  - The following example builds and starts 3 processes (run from within the `template_cpp` or the `template_java` directory):
```sh
# Build the application:
./build.sh

# In first terminal window:
./run.sh --id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/lcausal-broadcast.config

# In second terminal window:
./run.sh --id 2 --hosts ../example/hosts --output ../example/output/2.output ../example/configs/lcausal-broadcast.config

# In third terminal window:
./run.sh --id 3 --hosts ../example/hosts --output ../example/output/3.output ../example/configs/lcausal-broadcast.config

# Wait enough time for all processes to finish processing messages.
# Type Ctrl-C in every terminal window to create the output files.
```

Note: Refer to the project slides on how to use the `stress.py` tool to easily run your project.

### Limits
The entire project implements abstractions that operate in the asynchronous model, i.e., there is no bound in processing and communication delays. However, during the evaluation of the projects we set a maximum execution time limit.
In particular, for executions where:
- the network is not delaying/dropping/reordering packets and
- up to 9 processes broadcast 100 messages per each

we set the execution limit at 25 minutes.

Implementations that do not broadcast and do not deliver all messages within 25 minutes are considered incorrect.

### Cooperation
This project is meant to be completed individually. Copying from others is prohibited. You are free (and encouraged) to discuss the projects with others, but the submitted source code must be the exclusive work yours. Multiple copies of the same code will be disregarded without investigating which is the "original" and which is the "copy". Furthermore, please give appropriate credit to pieces of code you found online (e.g. in stackoverflow).

*Note*: code similarity tools will be used to check copying.

### Grading
This project accounts for 30% of the final grade and comprises three submissions:
  - A runnable application implementing Perfect Links (10%),
  - A runnable application implementing FIFO Broadcast (40%), and
  - A runnable application implementing Localized Causal Broadcast (50%).

Note that these submissions are *incremental*. This means that your work towards the first will help you in your work towards the second.

First, if your submission does not compile or invalid, e.g., produces empty output files, it will NOT be graded.
If your submission passes the initial validation, we will evaluate it based on two criteria: correctness and performance. We prioritize correctness: a correct implementation (i.e., that passes all the test cases) will receive (at least) a score of 4-out-of-6. The rest 2-out-of-6 is given based on the perfomance of your implementation compared to the perfomance of the implemantions submitted by your colleagues. The fastest correct implementation will receive a perfect score (6). Incorrect implementations receive a score below 4, depending on the number of tests they fail to pass.
