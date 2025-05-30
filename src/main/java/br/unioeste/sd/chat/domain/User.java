package br.unioeste.sd.chat.domain;

import lombok.*;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class User {
    private String username;
    private String ip;
}
