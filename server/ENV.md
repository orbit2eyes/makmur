# JWT_SECRET — Production Setup

## Required

`JWT_SECRET` must be set at startup. The server will refuse to start without it.

- Minimum 32 bytes (encoded, ~256 bits of entropy)
- Must be a base64 or hex string

## Generate

```bash
openssl rand -base64 32
```

Example output:

```
u3vPb8bY7kF2qXzR9mW1cN4oA5sD6fG0hJ2lK3pQ=
```

## Configure

### Environment variable

```bash
export JWT_SECRET="u3vPb8bY7kF2qXzR9mW1cN4oA5sD6fG0hJ2lK3pQ="
java -jar server/target/makmur-server-1.0.0.jar
```

### Command-line argument

```bash
java -jar server/target/makmur-server-1.0.0.jar --jwt.secret="u3vPb8bY7kF2qXzR9mW1cN4oA5sD6fG0hJ2lK3pQ="
```

### systemd service

```ini
[Service]
Environment=JWT_SECRET=u3vPb8bY7kF2qXzR9mW1cN4oA5sD6fG0hJ2lK3pQ=
ExecStart=/usr/bin/java -jar /opt/makmur/makmur-server-1.0.0.jar
```

### Docker

```dockerfile
ENV JWT_SECRET=u3vPb8bY7kF2qXzR9mW1cN4oA5sD6fG0hJ2lK3pQ=
```

Or pass at runtime:

```bash
docker run -e JWT_SECRET="u3vPb8bY7kF2qXzR9mW1cN4oA5sD6fG0hJ2lK3pQ=" ...
```

## Rotating the secret

1. Generate a new secret (same `openssl` command)
2. Restart the server with the new value
3. All existing JWTs become invalid — all users must re-login

## Security notes

- Never commit JWT_SECRET to git. Use a secrets manager (Vault, AWS Secrets Manager) or a `.env` file excluded from version control.
- Rotate periodically (every 90 days recommended).
- Use different secrets per environment (dev, staging, production).
