package dev.nlu.model;

import lombok.*;

import java.io.File;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileInfo {
    private int id;
    private int userId;
    private User user;
    private File file;
}
