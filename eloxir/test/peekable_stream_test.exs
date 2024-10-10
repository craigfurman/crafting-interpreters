defmodule PeekableStreamTest do
  use ExUnit.Case, async: true
  doctest PeekableStream

  test "can call next until underlying character stream is exhausted" do
    {:ok, iodev} = StringIO.open("foo")
    char_stream = IO.stream(iodev, 1)
    state = PeekableStream.new(char_stream)

    {char, state} = PeekableStream.next(state)
    assert char == "f"
    {char, state} = PeekableStream.next(state)
    assert char == "o"
    {char, state} = PeekableStream.next(state)
    assert char == "o"
    {char, _} = PeekableStream.next(state)
    assert char == :eof
  end

  test "can peek one character ahead without consuming it" do
    {:ok, iodev} = StringIO.open("fob")
    char_stream = IO.stream(iodev, 1)
    state = PeekableStream.new(char_stream)

    {char, state} = PeekableStream.peek(state)
    assert char == "f"
    {char, state} = PeekableStream.next(state)
    assert char == "f"
    {char, state} = PeekableStream.peek(state)
    assert char == "o"
    {char, _} = PeekableStream.next(state)
    assert char == "o"
  end
end
