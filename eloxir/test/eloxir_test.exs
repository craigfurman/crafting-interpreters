defmodule EloxirTest do
  use ExUnit.Case
  doctest Eloxir

  test "greets the world" do
    assert Eloxir.hello() == :world
  end
end
