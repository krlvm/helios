## 20.0704
gb: disable for now, buggy
helios: default to stereo sound
md: add svp support (using notaz implementation)
md: fix bug on vdp long reads
md: fix emulation stall on invalid vdp write
md: fix joypad detection 6btn
md: fix window plane
md: fix Z80 SP default value
md: avoid fm audio clipping
md: add satCache
md: add support for soft reset
md: only stop 68k on DMA MEM_TO_VRAM
md: savestate support for ssf2/sega mapper registers
md: support flat ROM mappers > 4MB
md: initial 128kb VRAM vdp support
md: support roms that require both ssf2 mapper and sram
sms: vdp handle tilemap mirroring
z80: use z80disasm from mame

## 20.0418
68k: add delay on z80 rom access
68k: basic prefetch impl
68k: do not consume an int when the level doesn't change
helios: add nes emulation based on the halfnes project
helios: backup sram, do not load empty files
helios: compute drift for more consistent framerates
helios: fix recent files handling
helios: load compressed roms
helios: mantain 4:3 aspect ratio by default
md: do not stop 68k on vdp fifo full, but only stop it if it tries to access vdp ports
md: fix dma fill when len=0
md: fm add dynamic rate control
md: improve shadow/highlight
md: use nuke Ym2612 emulation by default
md: savestates, use gsh format
md: support very small roms
sms: savestate fixes
ui: hide cursor on screen
ui: improve multi-screen support
ui: show info messages

## 19.1108
helios: update z80 lib
md: do not reset fm on z80 un-reset
md: for a given line, window plane hides planeA
md: improve interlace mode
md: tweak fm busy time
md: fix shadowHighlight
msx: add ASCII16, Konami mappers
ui: region selector, recent files
sms: support fm detection + infra
sms: fix mapper
sms: hint fix
sram: do not write an empty file

## 19.0706
68k: MOVEM fix pre-decrement long-writes on
gg: initial support and lots of fixes
md: fix z80 mem bounds
md: the path for sram files can now be changed via prop
md: improve cram viewer
sms: add support for MEKA rom db
sms: support pause button
sms: savestate and quicksave handling
emu: reset psg on load state
sms: support backup mapper
sms: support codem and korea mappers
sms: add support for V28-V30 video modes
sms: add EU/US support
ui: fix keyboard actions

## 19.0430
sg1000: add inital support + plenty of fixes
coleco: add initial support + plenty of fixes
msx: add initial support + plenty of fixes
sms: add initial support + plenty of fixes

emu: rename project to ... helios
emu: big infra rewrite to accomodate new systems
emu: load bios at startup if necessary

68k: fix movep.l issue
68k: fix flags for shift/rotate instructions when shift value = 0
msx: add ay38910 psg
msx: add carthw database
sound: default to 44.1khz
tms: add mode3


## 19.0217
* 68k: update 68k lib to latest and cleanup
* emu: reduce gc and perf tweaks
* emu: use better timers
* fm: refactor timers, increase precision
* psg: add attenuation property
* ui: handle save state to file
* ui: do not scale on EDT by default
* vdp: draw line on first slot
* vdp: fix dma copy bandwidth
* z80: interrupt work

## 19.0121
* 68k: fix buggy move.l
* 68k: move.l with a pre-decremented address register writes lsw and then msw
* app: fix close
* dma: fix 68kToVram
* emu: work on 68k and z80 timings
* emu: quick save states
* emu: initial savestate support
* emu: add ability to pause the emulation
* fifo: work in progress
* fm: busy flag and refactor
* pad: support for 6 buttons - disabled for now
* region: default to US
* ui: add pause and reset
* vdp: fix mid-frame cram updates
* vdp: fix headless mode
* vdp: fix rendering when sprites are close to the top/bottom
* vdp: vb on when !display
* vdp: dma bandwidth, consider REFRESH slots
* vdp: use slot granularity
* vdp: 8bit vram read
* vdp: hint fixes and tests
* z80: propagate un/reset to FM

## 18.1126
* fm: improve dac
* sound: close resources on exit
* emu: fix close and reload rom
* emu: default screen size scaling to 2x
* ui: add relevant help menus

## 18.1119
* 68k: rework Z-flag calc
* 68k: better handling of STOP, restart on exception
* 68k: RESET instruction should only reset external devices
* build: load version from manifest
* dma: fix fill and copy
* emu: basic cheat codes support
* emu: compute checksum and add autofix checksum
* fm: rework timers
* sram: write to file only on close
* ui: default scale set to 2
* vdp: rework rendering engine
* vdp: sprite masking, handle simple case
* vdp: shadow highlight - needs work
* vdp: refactor timings
* z80: fix reset logic

## v005
20181010  
* 68k and z80: use delays when handling vdp interrupts  
* 68k and z80: process interrupts when halted  
* 68k: fix f-line and a-line emulation  
* 68k: fix Zero flag when doing add.b or add.w  
* 68k: rebuild lib  
* emu: fix rom wrapping  
* emu: init I/O control port to 0 - fixes SGDK  
* fm: dac 13 bit, avoids clicks  
* sram: add save/load support  
* vdp: add support for VRAM 8-bit read  

## v004  
20180825    
* dma: rewrite and extract it to VpdDmaHandler  
* dma: initial support for DMA transfer rate  
* emu: add region override  
* ntsc: add support for V30 and H40  
* sram: refactor impl  
* vdp: do not set hblank on !displayEnable  
* vdp: extract memory handling to a separate class  
* z80: memory access fixes  

## v003  
20180803  
* 68k and z80: better error handling  
* eeprom: handle eeprom as sram  
* cart: ignore invalid SRAM setups  
* dma: refactor  
* emu: autodetect SSF2/Sega mapper  
* emu: initial headless support  
* fm: catch key_on exception  
* vdp: tweak hLinePassed  

## v002  
20180714  
* vdp: rebuild interrupt and hvc counter handling  
* fm: refactor timers  

## v001  
20180618  
* first public build  
