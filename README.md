# CZERTAINLY EJBCA NG Connector

> This repository is part of the open-source project CZERTAINLY. You can find more information about the project at [CZERTAINLY](https://github.com/CZERTAINLY/CZERTAINLY) repository, including the contribution guide.

EJBCA NG `Connector` is the implementation of the following `Function Groups` and `Kinds`:

| Function Group       | Kind                      |
|----------------------|---------------------------|
| `Authority Provider` | `EJBCA`                   |
| `Discovery Provider` | `EJBCA`, `EJBCA_SCHEDULE` |

EJBCA NG `Connector` is the implementation of certificate management for EJBCA that is compatible with the v2 client operations interface. The `Connector` is developed to work with SOAP Web Services calls.

> It is expected that the REST API calls will be implemented as option in the future release, because of some limitations of the EJBCA Web Service, for example limiting the number of end entities and certificates, that can be fetched.

EJBCA NG `Connector` allows you to perform the following operations:

`Authority Provider`
- Issue certificate
- Renew certificate
- Revoke certificate

`Discovery Provider`
- Discover certificates

## Database requirements

EJBCA NG `Connector` requires the PostgreSQL database to store the data.

## Short Process Description

EJBCA NG works under the principle of `RA Profiles`. The `Connector` provides the pathway for communication with the instances of EJBCA Certification Authorities. Multiple `Authorities` can be added using the same `Connector`. Once the `Authorities` are added, `RA Profiles` will be created on top of the `Authorities`.

With the help of `RA Profiles` and the CSR information provided by the `Client` using the REST API, the `Connector` communicates with the `Authority` to get the `Certificate`.

To know more about the `Core`, refer to [CZERTAINLY Core](https://github.com/CZERTAINLY/CZERTAINLY-Core)

## Certificate Discovery

The `Certificate` discovery in the EJBCA NG `Connector` works with the V2 `Certificate` Search API from EJBCA. Older versions of EJBCA that do not support V2 Search API are not supported.

There are two types of `Discovery`:
- `EJBCA`
- `EJBCA_SCHEDULE`

### `RA Profile` attributes

The attributes for creating a new `RA Profile` includes:
- End Entity Profile Name
- Certificate Profile Name
- Certificate Authority Name
- Key Recovery enabled/disabled
- Send Notifications enabled/disabled
- Username generation method
- Username generation prefix
- Username generation postfix

### Issue `Certificate` attributes

For issuing of new `Certificate`, you can use the following optional attributes for the End Entity:
- Email address
- Subject Alternative Names
- Extension data

The EJBCA username and attributes for to issue `Certificate` are written as `Metadata` in the `Certificate` object and can be used in future operations.

### Discover `Certificate` attributes

For discovering `Certificates` from the EJBCA, the following attributes can be used:
- Authority Instance Name
- API Base URL
- Certificate Authority
- Certificate Profile
- End Entity Profile
- Date after which the certificates were issued

## Interfaces

EJBCA NG `Connector` implements `v2 Authority Provider` and `Discovery Provider` interfaces. To learn more about the interfaces and end points, refer to the [CZERTAINLY Interfaces](https://github.com/CZERTAINLY/CZERTAINLY-Interfaces).

For more information, please refer to the [CZERTAINLY documentation](https://docs.czertainly.com).

## Docker container

EJBCA NG `Connector` is provided as a Docker container. Use the `docker pull czertainly/czertainly-ejbca-ng-connector:tagname` to pull the required image from the repository. It can be configured using the following environment variables:

| Variable                 | Description                                            | Required                                           | Default value |
|--------------------------|--------------------------------------------------------|----------------------------------------------------|---------------|
| `JDBC_URL`               | JDBC URL for database access                           | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `JDBC_USERNAME`          | Username to access the database                        | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `JDBC_PASSWORD`          | Password to access the database                        | ![](https://img.shields.io/badge/-YES-success.svg) | `N/A`         |
| `DB_SCHEMA`              | Database schema to use                                 | ![](https://img.shields.io/badge/-NO-red.svg)      | `ejbca`       |
| `PORT`                   | Port where the service is exposed                      | ![](https://img.shields.io/badge/-NO-red.svg)      | `8080`        |
| `TRUSTED_CERTIFICATES`   | List of PEM encoded additional trusted certificates    | ![](https://img.shields.io/badge/-NO-red.svg)      | `N/A`         |
| `REMOTE_DEBUG`           | Enables JVM remote debug on port 5005                  | ![](https://img.shields.io/badge/-NO-red.svg)      | `false`       |
| `MAX_PAYLOAD_SIZE`       | Maximum payload size in bytes                          | ![](https://img.shields.io/badge/-NO-red.svg)      | `2000000`     |
| `EJBCA_SEARCH_PAGE_SIZE` | Maximum number of certificates to fetch in one request | ![](https://img.shields.io/badge/-NO-red.svg)      | `100`         |
| `EJBCA_TIMEOUT_CONNECT`  | Connection timeout in milliseconds for EJBCA SOAP and REST API calls | ![](https://img.shields.io/badge/-NO-red.svg)      | `5000`        |
| `EJBCA_TIMEOUT_REQUEST`  | Request (read) timeout in milliseconds for EJBCA SOAP and REST API calls | ![](https://img.shields.io/badge/-NO-red.svg)  | `30000`       |

### Proxy settings

You may need to configure proxy to allow communication with external systems.
To enable proxy, use the following environment variables:

| Variable      | Description                                                                                                                                                | Required                                      | Default value |
|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------|---------------|
| `HTTP_PROXY`  | The proxy URL to use for http connections. Format: `<protocol>://<proxy_host>:<proxy_port>` or `<protocol>://<user>:<password>@<proxy_host>:<proxy_port>`  | ![](https://img.shields.io/badge/-NO-red.svg) | `N/A`         |
| `HTTPS_PROXY` | The proxy URL to use for https connections. Format: `<protocol>://<proxy_host>:<proxy_port>` or `<protocol>://<user>:<password>@<proxy_host>:<proxy_port>` | ![](https://img.shields.io/badge/-NO-red.svg) | `N/A`         |
| `NO_PROXY`    | A comma-separated list of host names that shouldn't go through any proxy                                                                                   | ![](https://img.shields.io/badge/-NO-red.svg) | `N/A`         |

Example values:
- `HTTP_PROXY=http://user:password@proxy.example.com:3128`
- `HTTPS_PROXY=http://user:password@proxy.example.com:3128`
- `NO_PROXY=localhost,127.0.0.1,0.0.0.0,10.0.0.0/8,cattle-system.svc,.svc,.cluster.local,my-domain.local`
