## 开机
## 查看docker是否已启动
ps aux | grep dockerd

若出现
``` bash

root        1247  0.5  0.9 3277020 38304 ?       Ssl  11:05   0:00 /usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock
phoenix     4911  0.0  0.0  17840  2456 pts/0    S+   11:07   0:00 grep --color=auto dockerd

```
这种/user/bin...表明启动成功

若没有,查看是否设置开机自启动
``` bash 
systemctl is-enabled docker
```
返回 `enabled` 表示已设为开机自启，`disabled` 则表示未设置。

如果发现未启动，手动启动：
``` bash
sudo systemctl start docker
```

若希望**开机自动启动**：
``` bash
sudo systemctl enable docker
```

## 启动Redis
