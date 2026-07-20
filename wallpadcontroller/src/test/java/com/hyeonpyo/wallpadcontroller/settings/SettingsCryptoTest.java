package com.hyeonpyo.wallpadcontroller.settings;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SettingsCryptoTest {
    @Test
    void encryptsWithRandomIvAndDecrypts() {
        SettingsCrypto crypto = new SettingsCrypto();
        crypto.initialize(SettingsCrypto.newKey());

        String first = crypto.encrypt("secret");
        String second = crypto.encrypt("secret");

        assertThat(first).isNotEqualTo("secret").isNotEqualTo(second);
        assertThat(crypto.decrypt(first)).isEqualTo("secret");
        assertThat(crypto.encrypt("")).isEmpty();
    }
}
