# Cybersecurity Assessment Lab
### Network Reconnaissance, Firewall Assessment, and Web Application Penetration Testing

## 1. Overview

The Cybersecurity Assessment Lab is a controlled security-testing environment designed to demonstrate how network-level and application-level security controls can be assessed, tested, and improved.

The project combines three areas of practical cybersecurity:

- **Network reconnaissance:** Identifying reachable hosts, open TCP ports, and exposed services using Nmap.
- **Firewall assessment:** Configuring and validating Uncomplicated Firewall (UFW) rules to control network traffic.
- **Web application penetration testing:** Identifying and exploiting an Insecure Direct Object Reference (IDOR) vulnerability in a local Spring Boot application, then verifying the effectiveness of the remediation.

The lab demonstrates how an exposed network service can create an entry point to an application, why network access controls alone cannot guarantee application security, and how security testing can verify protections at both layers.

All tests are conducted against virtual machines and application data created specifically for this lab.

## 2. Objectives

The objectives of this project are to:

- Discover network hosts and identify exposed services.
- Use Nmap to investigate open, closed, and filtered TCP ports.
- Configure UFW using a default-deny incoming policy.
- Validate firewall rules through external connectivity tests.
- Automate firewall configuration using Bash.
- Test authentication and resource-level authorisation in a web application.
- Reproduce an IDOR vulnerability using controlled HTTP requests.
- Verify that server-side ownership checks prevent unauthorised resource access.
- Record security incidents and test results.
- Compare system behaviour before and after remediation.
- Document findings, limitations, and recommendations.

## 3. Architecture

The lab consists of two Ubuntu virtual machines and a Spring Boot web application.

| Component | Role |
|---|---|
| Ubuntu-Project | Scanner and security-testing machine |
| Ubuntu-Target | Target machine hosting the firewall and, when integrated, the web application |
| Nmap | Network discovery, port scanning, and service detection |
| UFW | Network traffic filtering |
| Bash | Firewall configuration automation |
| Spring Boot | Web application and REST API |
| IDOR Hunter | Controlled application security testing |
| JUnit | Automated penetration-test and remediation verification |
| VirtualBox | Isolated virtualised lab environment |

### Network configuration

The virtual machines communicate through a VirtualBox host-only network.

| Machine | IP address |
|---|---|
| Ubuntu-Project | `192.168.56.101` |
| Ubuntu-Target | `192.168.56.102` |

The addresses must be verified against the actual network configuration before testing.

### Application architecture

IDOR Hunter is a Java and Spring Boot application using an embedded H2 database. It contains synthetic user accounts and documents to demonstrate broken access control without requiring an external database or real user data.

The application supports secure and intentionally vulnerable lab modes, automated HTTP-based security tests, incident logging, and optional administrator phone-call alerts.

## 4. Phase One — Network Reconnaissance

### Objective

Identify reachable hosts, discover exposed TCP ports, and determine which services are accessible on the target.

### Tools

- Nmap
- Ubuntu Linux
- VirtualBox host-only networking

### Host discovery

The target's reachability was checked using:

```bash
ping -c 4 192.168.56.102
```

A successful response indicates that the target is reachable through the tested network path. A failed response alone does not prove that a host is offline, because ICMP traffic may be filtered.

### Basic port scan

```bash
nmap 192.168.56.102
```

The scan identified TCP port 22 as open, indicating that an SSH service was accepting connections.

### Service and version detection

```bash
nmap -sV 192.168.56.102
```

The scan identified the SSH service and reported its detected OpenSSH version.

Service-version detection helps determine which software may require further investigation. A reported version alone does not prove that the service is vulnerable.

### Operating system detection

```bash
sudo nmap -O 192.168.56.102
```

Nmap produced a Linux operating-system fingerprint but did not identify the exact operating system with certainty.

### Findings

The initial reconnaissance established a baseline of the target's network exposure.

The principal finding was an accessible SSH service on TCP port 22. This service is legitimate for remote administration but should be restricted to authorised users and networks.

**Security significance:** Network reconnaissance identifies the services that may require further assessment. It does not, by itself, demonstrate a successful compromise.

## 5. Phase Two — Firewall Configuration and Assessment

### Objective

Configure UFW to restrict incoming network connections, then verify whether the observed traffic behaviour matches the intended firewall policy.

### Firewall policy

The target was configured to deny incoming connections by default while allowing outgoing connections.

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
```

The following rules were configured:

| Port | Service | Policy | Reason |
|---|---|---|---|
| 22/tcp | SSH | Allow | Remote administration |
| 80/tcp | HTTP | Deny | Restrict unencrypted web access |
| 443/tcp | HTTPS | Allow | Permit secure web traffic |
| 8080/tcp | Application/development services | Deny | Restrict direct access to services on this port |

The rules demonstrate that different services can receive different network-access policies.

Allowing port 443 does not automatically establish that a correctly configured HTTPS service is running. Similarly, denying port 80 does not itself configure HTTPS or redirect HTTP traffic to HTTPS.

### Enabling and verifying UFW

```bash
sudo ufw enable
sudo ufw status verbose
```

The firewall status was inspected to confirm that UFW was active and the expected rules were present.

### Automated configuration

The Bash script `ufw_configuration.sh` automates the firewall configuration.

It sets the default policies, configures the required port rules, enables UFW, and displays the resulting status.

To execute the script:

```bash
chmod +x ufw_configuration.sh
sudo ./ufw_configuration.sh
```

The script should be reviewed before execution because firewall changes can affect remote connectivity.

### Traffic verification

From Ubuntu-Project, the following commands were used to test the target:

```bash
nmap -p 22 192.168.56.102
nmap -p 80 192.168.56.102
nmap -p 8080 192.168.56.102
```

The observed results were:

- TCP port 22 remained accessible.
- TCP port 80 was reported as filtered.
- TCP port 8080 was reported as filtered.

An `open` result generally indicates that a service is accepting connections. A `filtered` result means Nmap could not determine whether the port was open because packet filtering or another network obstacle prevented a definitive response.

### Firewall assessment

The assessment compares the intended firewall configuration with the externally observed network behaviour.

A discrepancy may indicate a configuration error, an unexpected service exposure, or another network condition that requires investigation.

Potential checks include:

- Confirming that the active firewall rules match the documented policy.
- Checking for unintended allow rules.
- Checking whether services are listening on unexpected ports or network interfaces.
- Verifying whether a service is reachable from networks that should not have access.
- Checking whether a firewall rule is too broad or permits more source addresses than necessary.

The current baseline demonstrates traffic filtering and configuration verification. A firewall bypass is not claimed unless a test actually demonstrates access contrary to the intended policy.

## 6. Phase Three — Controlled Web Application Penetration Testing

### Objective

Assess whether the web application correctly enforces authentication and resource-level authorisation.

### What is IDOR?

Insecure Direct Object Reference (IDOR) is an access-control vulnerability in which an application allows a user to access a resource without correctly verifying that the user is authorised to access it.

For example, User A may be allowed to access document `101`, while document `202` belongs to User B.

If User A changes the document ID in a request and the server returns User B's private document, the application has failed to enforce the required ownership check.

The vulnerability is not the visibility of the identifier itself. The vulnerability is the failure to enforce authorisation on the requested resource.

### Application setup

IDOR Hunter uses Java, Spring Boot, Spring Security, and an embedded H2 database.

It provides synthetic user accounts with separate documents and supports secure and vulnerable lab modes.

The application starts locally using:

```bash
mvn clean install
mvn spring-boot:run
```

By default, the application is available at:

`http://localhost:8080`

The actual network address and port depend on the deployment configuration.

### Manual IDOR test

In the controlled lab:

1. Authenticate as User A.
2. Open a document owned by User A.
3. Inspect the HTTP request used to retrieve the document.
4. Change the document identifier to a known document belonging to User B.
5. Inspect the HTTP response and verify whether User B's private data is returned.
6. Repeat the test in secure mode.
7. Compare the response and confirm whether the ownership check prevents unauthorised access.

The test is performed only against the synthetic accounts and documents provided by the lab.

### Automated penetration-test suite

IDOR Hunter includes a Java JUnit suite that sends HTTP requests to the application and checks both response status and response content.

| Test scenario | Expected result |
|---|---|
| User accesses their own document | `200 OK` |
| User attempts cross-user access in secure mode | `403 Forbidden`, with no private document data exposed |
| User attempts cross-user access in vulnerable mode | `200 OK`, with the other user's document data exposed |
| User requests a nonexistent document | `404 Not Found` |
| Unauthenticated request | `401 Unauthorized` |

The vulnerable-mode test is designed to confirm actual data exposure rather than treating any successful HTTP response as proof of a vulnerability.

A failed cross-user test in vulnerable mode is an expected demonstration of the security weakness. The same test should pass in secure mode when the application correctly blocks the request.

## 7. Phase Four — Remediation and Verification

### Application remediation

The IDOR vulnerability is addressed by enforcing resource ownership and applicable role permissions on the server.

The application must not rely on a hidden identifier, a frontend restriction, or the assumption that a user will not change a request.

After remediation, the same cross-user request is repeated. The expected result is that the application denies access without disclosing the protected document's content.

### Firewall remediation

Firewall findings should be addressed by correcting the relevant UFW rules, restricting source addresses where appropriate, and disabling unnecessary listening services.

After a firewall change, the same network tests should be repeated to verify the resulting behaviour.

### Before-and-after comparison

The combined lab uses two distinct forms of verification:

| Security layer | Evidence |
|---|---|
| Network | Nmap results and UFW status before and after a firewall change |
| Application | HTTP status codes, response bodies, and automated JUnit results before and after access-control remediation |

The two types of evidence should not be confused. A blocked network connection does not prove that the application is secure, and a correctly enforced application permission does not prove that the network is properly configured.

## 8. Incident Logging and Alerts

IDOR Hunter records relevant security events through its incident-management components.

The lab distinguishes between blocked cross-user access attempts and confirmed breaches.

- Blocked attempts are recorded as lower-severity events.
- Confirmed unauthorised data exposure is recorded as a critical incident.
- Confirmed breaches can trigger an alert cycle.
- The retry scheduler limits repeated notification attempts.
- An administrator can acknowledge an alert to stop further retries.

The default telephony provider is a mock implementation. It does not place real phone calls.

Real Twilio calls are optional and require explicit configuration and valid credentials. Automated tests use the mock provider to avoid making external calls.

## 9. Security Findings and Recommendations

### Network security

An open port is not automatically a vulnerability. Each exposed service should be justified by an operational requirement and protected by appropriate authentication and configuration.

Recommended controls include:

- Restricting SSH to authorised users and trusted source addresses.
- Disabling unnecessary services.
- Applying firewall rules according to least privilege.
- Keeping the operating system and network services updated.
- Verifying firewall behaviour from an independent testing machine.

### Web application security

Authentication alone does not establish that a user may access every resource.

Recommended controls include:

- Enforcing resource ownership checks on the server.
- Applying role-based authorisation where required.
- Testing access to resources belonging to other users.
- Avoiding reliance on client-side restrictions.
- Verifying that error responses do not disclose private information.
- Maintaining repeatable automated security tests.

### Defence in depth

Network controls and application controls provide different protections.

A firewall can limit which machines and services can communicate. Application-level authorisation determines what an authenticated user can do once a request reaches the application.

Both layers must be configured and tested independently.

## 10. Tools and Technologies

| Technology | Purpose |
|---|---|
| Ubuntu Linux | Lab operating system |
| VirtualBox | Isolated virtual machines |
| Nmap | Network reconnaissance and port testing |
| UFW | Firewall configuration and traffic filtering |
| Bash | Firewall automation |
| Nginx | HTTP service for firewall testing |
| Java 21 | Application and security-test implementation |
| Spring Boot | Web application and REST API |
| Spring Security | Authentication and security configuration |
| H2 Database | Embedded application database |
| JUnit | Automated security testing |
| Twilio (optional) | External phone-call notifications |

## 11. Repository Structure

The combined repository can be organised as follows:

```text
cybersecurity-assessment-lab/
├── README.md
├── network-reconnaissance/
│   ├── README.md
│   └── screenshots/
├── ufw-firewall-configuration/
│   ├── README.md
│   ├── ufw_configuration.sh
│   └── screenshots/
└── idor-hunter/
    ├── pom.xml
    ├── .env.example
    ├── README.md
    └── src/
        ├── main/
        └── test/
```

The individual project folders can retain their detailed documentation, while this top-level README explains how the projects relate to each other.

## 12. Verification Status and Limitations

The original Nmap and UFW exercises document the commands, configuration and observations from the local virtual-machine environment.

IDOR Hunter documents the intended behaviour of its vulnerable and secure modes, its automated penetration-test suite, incident logging, and optional alerting.

The application README states that compilation and test execution were not completed in the original authoring environment because Maven and the required dependency cache were unavailable. Therefore, the documented test output must not be presented as independently verified until the tests have actually been run.

The combined architecture also requires an integration step if the Spring Boot application is to run on Ubuntu-Target and be tested remotely from Ubuntu-Project. The existing firewall configuration denies incoming traffic on port 8080, so the web application will not be remotely accessible through that port until an appropriate lab rule is deliberately configured.

A controlled integration test should verify both conditions:

1. The application port is unreachable when the firewall policy denies access.
2. The application becomes reachable from the intended testing machine after a narrowly scoped allow rule is applied, after which application-level security tests can run.

This demonstrates the difference between network reachability and application authorisation without claiming that the firewall has been bypassed.

All tests must remain confined to systems owned by the project author or explicitly authorised for testing.

## 13. Ethical Considerations

The lab intentionally includes a vulnerable application mode for educational purposes.

Vulnerable mode must only be used with synthetic data in a controlled environment. It must never be exposed to an untrusted network.

The project is intended to develop practical skills in reconnaissance, firewall assessment, web application penetration testing, secure coding, and remediation verification.

## 14. Conclusion

The Cybersecurity Assessment Lab demonstrates a layered approach to security assessment.

Nmap is used to discover network exposure, UFW is configured and tested to control incoming traffic, and IDOR Hunter is used to investigate application-level access-control weaknesses.

The project highlights that network filtering and application authorisation address different threats. A secure system requires both appropriately configured network controls and correctly enforced application permissions.

By documenting the initial configuration, test procedure, observed evidence, remediation, and repeat verification, the lab demonstrates a repeatable approach to identifying and addressing security weaknesses in a controlled environment.