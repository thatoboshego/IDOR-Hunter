# UFW Firewall Configuration and Traffic Filtering

## Overview

This project demonstrates the configuration and testing of the Uncomplicated Firewall (UFW) on an Ubuntu Linux system.

The objective was to configure firewall rules to control incoming network traffic, allow required services, block unwanted traffic, and verify that the firewall rules were working correctly.

The firewall was configured on an Ubuntu target virtual machine and tested from a separate Ubuntu virtual machine using Nmap. A Bash script was also created to automate the firewall configuration.

---

## Objectives

The objectives of this project were to:

* Install and configure UFW.
* Enable UFW and configure it to start automatically.
* Allow SSH traffic.
* Deny HTTP traffic.
* Allow HTTPS traffic.
* Deny traffic on port 8080.
* Configure default incoming and outgoing firewall policies.
* Verify the configured firewall rules.
* Test allowed and blocked network traffic.
* Automate the firewall configuration using Bash.

---

## Tools and Technologies

| Tool         | Purpose                                      |
| ------------ | -------------------------------------------- |
| Ubuntu Linux | Operating system used for the security lab   |
| UFW          | Firewall configuration and traffic filtering |
| Nmap         | Network connectivity and port testing        |
| Nginx        | HTTP service used to test port 80 filtering  |
| Bash         | Firewall automation                          |
| VirtualBox   | Virtual machine environment                  |

---

## Lab Environment

Two Ubuntu virtual machines were used for this project.

| Machine        | Role                         | IP Address       |
| -------------- | ---------------------------- | ---------------- |
| Ubuntu-Project | Scanning and testing machine | `192.168.56.101` |
| Ubuntu-Target  | Firewall-protected machine   | `192.168.56.102` |

The Ubuntu-Target machine was configured with UFW.

The Ubuntu-Project machine was used to test network connectivity and verify the firewall rules using Nmap.

Both virtual machines communicated through a VirtualBox host-only network.

---

## Firewall Configuration

### Default Policies

The firewall was configured to deny incoming connections by default while allowing outgoing connections.

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
```

This follows a default-deny approach where incoming traffic must be explicitly allowed.

### SSH — Allow

SSH traffic was allowed on port 22:

```bash
sudo ufw allow 22/tcp
```

SSH was allowed because it is required for remote administration of the target system.

### HTTP — Deny

HTTP traffic was denied on port 80:

```bash
sudo ufw deny 80/tcp
```

This demonstrates how a firewall can prevent access to an HTTP service.

### HTTPS — Allow

HTTPS traffic was allowed on port 443:

```bash
sudo ufw allow 443/tcp
```

HTTPS was allowed as an example of permitting a secure web service.

### Port 8080 — Deny

Traffic to port 8080 was denied:

```bash
sudo ufw deny 8080/tcp
```

This demonstrates how access to a specific application or service port can be restricted.

---

## Enabling UFW

After configuring the firewall rules, UFW was enabled:

```bash
sudo ufw enable
```

UFW was also configured to remain enabled when the system starts.

The active firewall configuration was verified using:

```bash
sudo ufw status verbose
```

### UFW Active Status

![UFW Active Status](screenshots/ufw-status-active.png)

### Final UFW Rules

![Final UFW Rules](screenshots/ufw-rules-final.png)

---

## Traffic Testing

The firewall rules were tested from the Ubuntu-Project machine using Nmap.

### SSH — Allowed

Port 22 was tested using:

```bash
nmap -p 22 192.168.56.102
```

The SSH port remained accessible because it was explicitly allowed by the firewall.

![SSH Allowed](screenshots/ssh-allowed.png)

### HTTP — Blocked

Port 80 was tested using:

```bash
nmap -p 80 192.168.56.102
```

The port was reported as filtered because UFW was configured to deny HTTP traffic.

![HTTP Blocked](screenshots/http-blocked.png)

### Port 8080 — Blocked

Port 8080 was tested using:

```bash
nmap -p 8080 192.168.56.102
```

The port was reported as filtered because UFW was configured to deny traffic to port 8080.

![Port 8080 Blocked](screenshots/8080-blocked.png)

---

## HTTP Testing Environment

Nginx was installed on the Ubuntu-Target machine to provide an actual HTTP service on port 80.

It was started using:

```bash
sudo systemctl start nginx
```

This provided a real network service against which the HTTP firewall rule could be tested.

---

## Automated Firewall Configuration

The project includes a Bash script named:

```text
ufw_configuration.sh
```

The script applies the firewall configuration automatically.

It performs the following actions:

1. Sets the default incoming policy to deny.
2. Sets the default outgoing policy to allow.
3. Allows SSH on port 22.
4. Denies HTTP on port 80.
5. Allows HTTPS on port 443.
6. Denies port 8080.
7. Enables UFW.
8. Displays the firewall status.

The script can be made executable with:

```bash
chmod +x ufw_configuration.sh
```

It can then be executed with:

```bash
./ufw_configuration.sh
```

### Script Verification

The firewall was reset and the configuration script was executed to verify that the rules could be recreated automatically.

![UFW Script Verification](screenshots/ufw-script-verification.png)

---

## Security Considerations

A firewall provides an important layer of network security by controlling which connections are permitted to reach a system.

Using a default-deny incoming policy reduces unnecessary exposure because services are not accessible unless they have been explicitly permitted.

Allowing only required services follows the principle of least privilege.

A firewall is not a complete security solution and should be combined with other security controls, including:

* Strong authentication
* Secure passwords or SSH keys
* Regular software updates
* Service hardening
* Network monitoring
* Intrusion detection
* Secure application configuration

---

## Lessons Learned

This project provided practical experience with:

* Linux firewall configuration.
* UFW firewall rules and policies.
* TCP ports and network services.
* Allowing and denying network traffic.
* Using Nmap to verify firewall behaviour.
* Understanding the difference between open and filtered ports.
* Automating security configuration with Bash.
* Testing security controls rather than relying only on configuration output.
* Applying the principle of least privilege to network access.

---

## Project Structure

```text
Task-2/
├── README.md
├── ufw_configuration.sh
└── screenshots/
    ├── ufw-status-active.png
    ├── ssh-allowed.png
    ├── http-blocked.png
    ├── 8080-blocked.png
    ├── ufw-rules-final.png
    └── ufw-script-verification.png
```

---

## Ethical Considerations

All scanning and firewall testing performed in this project was conducted against virtual machines owned and controlled for educational purposes.

Nmap and other security tools should only be used against systems for which you have explicit authorization.

Unauthorized scanning or testing of systems can disrupt services and may violate applicable laws and policies.

---

## Conclusion

This project demonstrated how UFW can be used to control network access on an Ubuntu system.

SSH was explicitly allowed for administration, HTTP and port 8080 were blocked, and HTTPS was allowed as an example of permitting a secure service.

The firewall configuration was verified using UFW and tested from a separate Ubuntu machine using Nmap. The configuration was also automated using a Bash script, making the setup reproducible.

The project demonstrates practical application of network security principles including access control, least privilege, firewall configuration, traffic filtering, and security verification.
