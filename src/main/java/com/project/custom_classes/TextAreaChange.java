package com.project.custom_classes;

public record TextAreaChange(diff_match_patch.Operation operation, String text,  int newPosition) {
}
