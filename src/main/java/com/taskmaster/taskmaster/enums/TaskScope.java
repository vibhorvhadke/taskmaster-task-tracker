package com.taskmaster.taskmaster.enums;

/** Which tasks to list. */
public enum TaskScope {
    ALL,              // every task I can see (my teams' tasks + my personal tasks)
    ASSIGNED_TO_ME,
    CREATED_BY_ME
}