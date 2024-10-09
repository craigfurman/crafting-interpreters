defmodule PeekableStreamTest do
  use ExUnit.Case, async: true
  doctest PeekableStream

  test "can call next until underlying character stream is exhausted" do
    {:ok, iodev} = StringIO.open("foo")
    char_stream = IO.stream(iodev, 1)
    peek_stream_pid = PeekableStream.new(char_stream)

    char = PeekableStream.next(peek_stream_pid)
    assert char == "f"
    char = PeekableStream.next(peek_stream_pid)
    assert char == "o"
    char = PeekableStream.next(peek_stream_pid)
    assert char == "o"
    char = PeekableStream.next(peek_stream_pid)
    assert char == :eof
  end

  test "can peek one character ahead without consuming it" do
    {:ok, iodev} = StringIO.open("foo")
    char_stream = IO.stream(iodev, 1)
    peek_stream_pid = PeekableStream.new(char_stream)

    char = PeekableStream.peek(peek_stream_pid)
    assert char == "f"
    char = PeekableStream.next(peek_stream_pid)
    assert char == "f"
    char = PeekableStream.next(peek_stream_pid)
    assert char == "o"
  end
end
