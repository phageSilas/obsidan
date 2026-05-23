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

## 查看docker中正在运行的进程
``` bash
docker ps 
```

## 启动Redis
查看docker中Redis是否已启动
``` bash
docker ps -a --filter name=redis
```

``` bash
 ID   IMAGE     COMMAND                   CREATED      STATUS         PORTS                                         NAMES
a6e43522ef0f   redis     "docker-entrypoint.s…"   3 days ago   Up 9 minutes   0.0.0.0:6379->6379/tcp, [::]:6379->6379/tcp   redis
```
看到STATUS和Up 表明启动成功

否则 进行设置Redis随Docker自启动
``` bash
 docker update --restart=always redis
 
 # 输出
 redis 或什么也不显示
```

检查是否设置自启动成功
``` bash
docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' redis
```
 输出：always 表明设置成功

## 启动PostgreSql
给 PostgreSQL 容器设置自启动，和 Redis 完全一样，也是通过**重启策略**来实现。

先用 `docker ps -a` 找到 PostgreSQL 的容器名
```bash
docker ps -a (该命令是查看docker中所有包括已停止的容器)
```
在输出中找到 `IMAGE` 列包含 `postgres` 的行，记下postgres的 `NAMES`


设置重启策略为 `always`
```bash
docker update --restart=always postgres
```

 验证是否设置成功
```bash
docker inspect -f '{{.HostConfig.RestartPolicy.Name}}' postgres
```
输出 `always` 即表示成功。

