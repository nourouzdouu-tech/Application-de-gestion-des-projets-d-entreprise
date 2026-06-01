package com.dxc.dxc_platform.service;

public interface WebAuthnService {
    String startRegistration(String email) throws Exception;
    void finishRegistration(String email, String credentialJson) throws Exception;
    String startLogin(String email) throws Exception;
    String finishLogin(String email, String assertionJson) throws Exception;
    boolean hasCredential(String email);
}