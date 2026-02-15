package dev.nlu.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
//@ToString (exclude = "password")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private int id;
    private String username;
    private String password;
}
