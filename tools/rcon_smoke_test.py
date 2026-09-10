# -*- coding: utf-8 -*-
"""
取出接口连通搜索的端到端冒烟测试（一次性脚本，需服务端开启 RCON）。

搭建 (0,100,0)=取出接口 → (0,100,1)=ATI耕地(导体) → (0,100,2)=存储方块制造机(源)，
在接口正下方放漏斗从接口抽物品，用漏斗内容判断聚合是否生效：
  1) 连通时应抽到铁锭
  2) 把导体换成石头后应抽不到
  3) 恢复导体后应重新抽到
"""
import socket
import struct
import sys
import time

HOST, PORT, PASSWORD = "127.0.0.1", 25575, "ati"


class Rcon:
    def __init__(self):
        self.sock = socket.create_connection((HOST, PORT), timeout=10)
        self.rid = 0
        self._send(3, PASSWORD)
        while True:
            rid, typ, body = self._recv()
            if typ == 2:
                break

    def _read(self, n):
        buf = b""
        while len(buf) < n:
            chunk = self.sock.recv(n - len(buf))
            if not chunk:
                raise IOError("连接已关闭")
            buf += chunk
        return buf

    def _send(self, typ, body):
        self.rid += 1
        data = struct.pack("<ii", self.rid, typ) + body.encode("utf8") + b"\x00\x00"
        self.sock.sendall(struct.pack("<i", len(data)) + data)

    def _recv(self):
        (length,) = struct.unpack("<i", self._read(4))
        data = self._read(length)
        rid, typ = struct.unpack("<ii", data[:8])
        return rid, typ, data[8:-2].decode("utf8", "replace")

    def cmd(self, command):
        self._send(2, command)
        _, _, body = self._recv()
        return body.strip()


def setup(r):
    print("[搭建] 强制加载区块并放置测试方块")
    r.cmd("forceload add 0 0 32 32")
    time.sleep(2)
    # 先清空再放置，保证每次运行都从干净状态开始（方块相同时 setblock 会失败）
    for c in [
        "setblock 0 100 0 minecraft:air",
        "setblock 0 99 0 minecraft:air",
        "setblock 0 100 1 minecraft:air",
        "setblock 0 100 2 minecraft:air",
    ]:
        r.cmd(c)
    time.sleep(1)
    for c in [
        "setblock 0 100 0 alltheimbaium:extraction_interface",
        "setblock 0 99 0 minecraft:hopper[facing=down]",
        "setblock 0 100 1 alltheimbaium:farmland",
        "setblock 0 100 2 alltheimbaium:storage_fountain",
        # 给源机器注入 2 件存量 + 每 tick 5 件的产量，无需 GUI 标记
        'data merge block 0 100 2 {save_stick:[{id:"minecraft:iron_ingot",Count:1b,Long_Count:2000L}],output:5000L,outputEnabled:0b}',
    ]:
        print("   >", c, "->", r.cmd(c))


def has_items(r):
    """漏斗里是否有东西。空时为 "... block data: []"，非空为 "... block data: [{...}]" """
    out = r.cmd("data get block 0 99 0 Items")
    marker = "block data: "
    idx = out.find(marker)
    payload = out[idx + len(marker):].strip() if idx >= 0 else ""
    return payload.startswith("[{"), out


def main():
    r = Rcon()
    print("已连接 RCON:", r.cmd("list"))
    setup(r)

    print("\n[用例1] 连通状态下等待 6 秒，漏斗应能抽到物品")
    time.sleep(6)
    ok1, out1 = has_items(r)
    print("   漏斗:", out1[:200])
    print("   结果:", "通过" if ok1 else "失败 —— 聚合没有生效")

    # 拓扑变更后连通范围最多 1 秒后重算，先等重算完成再清空漏斗，
    # 否则会把重算延迟内的正常抽取误判成"仍然连通"
    print("\n[用例2] 把导体换成石头，等重算完成后清空漏斗再等 6 秒，应抽不到")
    r.cmd("setblock 0 100 1 minecraft:stone")
    time.sleep(3)
    r.cmd("data merge block 0 99 0 {Items:[]}")
    time.sleep(6)
    ok2, out2 = has_items(r)
    print("   漏斗:", out2[:200])
    print("   结果:", "通过" if not ok2 else "失败 —— 搜索穿过了非本模组方块")

    print("\n[用例3] 恢复导体，等重算完成后清空漏斗再等 6 秒，应重新抽到")
    r.cmd("setblock 0 100 1 alltheimbaium:farmland")
    time.sleep(3)
    r.cmd("data merge block 0 99 0 {Items:[]}")
    time.sleep(6)
    ok3, out3 = has_items(r)
    print("   漏斗:", out3[:200])
    print("   结果:", "通过" if ok3 else "失败 —— 恢复导体后未重新连通")

    # 零刻熔炉刻意不是产出源：它只传导，其中的物品不该被抽走
    print("\n[用例4] 只放零刻熔炉（内含物品）作为接口的唯一本模组邻居，应抽不到")
    for c in ["setblock 0 100 1 minecraft:air", "setblock 0 100 2 minecraft:air"]:
        r.cmd(c)
    time.sleep(1)
    r.cmd("setblock 0 100 1 alltheimbaium:instant_furnace")
    r.cmd('data merge block 0 100 1 {input:[{id:"minecraft:iron_ingot",Count:1b,Stock:5000L}],'
          'output:[{id:"minecraft:iron_ingot",Count:1b,Stock:5000L}],energy:1000000}')
    time.sleep(3)
    r.cmd("data merge block 0 99 0 {Items:[]}")
    time.sleep(6)
    ok4, out4 = has_items(r)
    print("   漏斗:", out4[:200])
    print("   结果:", "通过" if not ok4 else "失败 —— 熔炉被当成了产出源")

    print("\n[用例5] 取出接口 → 零刻熔炉 → 存储方块制造机，熔炉应能传导，仍应抽到")
    r.cmd("setblock 0 100 2 alltheimbaium:storage_fountain")
    r.cmd('data merge block 0 100 2 {save_stick:[{id:"minecraft:iron_ingot",Count:1b,Long_Count:2000L}],output:5000L,outputEnabled:0b}')
    time.sleep(3)
    r.cmd("data merge block 0 99 0 {Items:[]}")
    time.sleep(6)
    ok5, out5 = has_items(r)
    print("   漏斗:", out5[:200])
    print("   结果:", "通过" if ok5 else "失败 —— 熔炉没有起到传导作用")

    passed = ok1 and not ok2 and ok3 and not ok4 and ok5
    print("\n==== 总判定:", "全部通过" if passed else "存在失败项", "====")
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
