Result: Blocked.

The D-source ordinary Web build succeeded and the current public `/zhj/` page remained reachable with HTTP `200`, but the release could not be uploaded because the SSH user lacks write permission under `/opt/sxyq27/releases`, non-interactive sudo is unavailable, and root SSH is rejected. The existing symlink and Nginx configuration were not changed.

Unblock condition: provide an authorized deployment path for the existing 124 Nginx release structure, then rerun only the Web upload, symlink, reload and hash checks. No management backend release is included in this case.
