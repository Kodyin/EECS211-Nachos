# EECS211 - Nachos
JAVA Nachos
EECS211 2018 Winter term project


Author: Lu Hongjian, Ma Yishan, Tian Luqi, Wang Gaozhiquan, Wang Ruihong

## Project target

Build a OS prototype.


## Instruction

Compiling Nachos:

First, put the 'nachos/bin' directory on your PATH. This directory
contains the script 'nachos', which simply runs the Nachos code.

To compile Nachos, go to the subdirectory for the project you wish 
to compile (I will assume 'proj1/' for Project 1 in my examples), 
and run:

	gmake

This will compile those portions of Nachos which are relevant to the
project, and place the compiled .class files in the proj1/nachos
directory. 

You can now test Nachos from the proj1/ directory with:

	nachos

If you are working on a project which runs user programs (projects 2-4), 
you will also need to compile the MIPS test programs with:

	gmake test

Command Line Arguments:

For a summary of the command line arguments, run:

	nachos -h

The commands are:

        -d <debug flags>
                Enable some debug flags, e.g. -d ti

        -h
                Print this help message.

        -s <seed>
                Specify the seed for the random number generator

        -x <program>
		Specify a program that UserKernel.run() should execute,
		instead of the value of the configuration variable
		Kernel.shellProgram

        -z
                print the copyright message

        -- <grader class>
		Specify an autograder class to use, instead of
		nachos.ag.AutoGrader

        -# <grader arguments>
                Specify the argument string to pass to the autograder.

        -[] <config file>
                Specifiy a config file to use, instead of nachos.conf


Nachos offers the following debug flags:

    c: COFF loader info 
    i: HW interrupt controller info 
    p: processor info 
    m: disassembly 
    M: more disassembly 
    t: thread info 
    a: process info (formerly "address space", hence a) 

To use multiple debug flags, clump them all together. For example, to
monitor coff info and process info, run:

	nachos -d ac

## More
...

