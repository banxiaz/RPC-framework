package com.bai;

import lombok.*;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class HelloEntity implements Serializable {
    private String message;
    private String description;
}
